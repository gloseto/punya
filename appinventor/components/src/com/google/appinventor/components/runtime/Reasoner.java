package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.PunyaVersion;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.IOUtils;
import com.google.appinventor.components.runtime.util.MediaUtil;
import com.google.appinventor.components.runtime.util.YailDictionary;
import com.google.appinventor.components.runtime.util.YailList;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.ModelFactory2;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.reasoner.rulesys.FBRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.reasoner.rulesys.RuleReasoner;
import com.hp.hpl.jena.vocabulary.RDF;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * The <code>Reasoner</code> component derives statements from the contents of a
 * <code>LinkedData</code> component.
 *
 * @author Evan W. Patton (ewpatton@mit.edu)
 */
@DesignerComponent(version = PunyaVersion.REASONER_COMPONENT_VERSION,
    nonVisible = true,
    category = ComponentCategory.LINKEDDATA,
    iconName = "images/reasoner.png"
)
@SimpleObject
@UsesLibraries({"jena-reasoner.jar"})
public class Reasoner extends LinkedDataBase<InfModel> {

  private static final String LOG_TAG = Reasoner.class.getSimpleName();
  private LinkedData basemodel = null;
  private String rulesEngine = "";
  private String rulesFile = "";
  private List<Rule> rules = new ArrayList<>();

  /**
   * Creates a new Reasoner..
   *
   * @param form the container that this component will be placed in
   */
  public Reasoner(Form form) {
    super(form);
  }

  ///region Properties

  @SimpleProperty
  public LinkedData Model() {
    return basemodel;
  }

  /**
   * Specifies the base model (A-Box + T-Box) for reasoning.
   */
  @DesignerProperty(
      editorType = PropertyTypeConstants.PROPERTY_TYPE_COMPONENT + ":com.google.appinventor.components.runtime.LinkedData"
  )
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void Model(LinkedData model) {
    this.basemodel = model;
  }

  @SimpleProperty
  public String RulesEngine() {
    return "";
  }

  /**
   * Specifies the base semantics that should be used for reasoning.
   *
   * @param rules the base semantics to use for reasoning
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_CHOICES,
      editorArgs = {"None", "RDFS", "OWL Micro", "OWL Mini", "OWL"},
      defaultValue = "None")
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void RulesEngine(String rules) {
    this.rulesEngine = rules;
  }

  @SimpleProperty
  public String RulesFile() {
    return rulesFile;
  }

  /**
   * An optional file containing custom rules to be used during reasoning. The rules are applied
   * in addition to any reasoning applied by the choice of {@link #RulesEngine}.
   *
   * @param rules path to an additional ruleset to include for reasoning
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_ASSET)
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void RulesFile(String rules) {
    this.rulesFile = rules;
  }

  ///endregion
  ///region Methods

  /**
   * Runs the reasoner in forward chaining model to derive conclusions. On success, the
   * {@link #ReasoningComplete} event will run. {@link #ErrorOccurred} event will run if reasoning
   * fails.
   */
  @SimpleFunction
  public void Run() {
    if (this.basemodel == null) {
      form.dispatchErrorOccurredEvent(this, "Run", 0);
      return;
    }
    final String rulesFile = this.rulesFile;
    final String rulesEngine = this.rulesEngine;
    AsynchUtil.runAsynchronously(new Runnable() {
      @Override
      public void run() {
        try {
          com.hp.hpl.jena.reasoner.Reasoner reasoner;
          if ("RDFS".equals(rulesEngine)) {
            reasoner = ReasonerRegistry.getRDFSReasoner();
          } else if ("OWL Micro".equals(rulesEngine)) {
            reasoner = ReasonerRegistry.getOWLMicroReasoner();
          } else if ("OWL Mini".equals(rulesEngine)) {
            reasoner = ReasonerRegistry.getOWLMiniReasoner();
          } else if ("OWL".equals(rulesEngine)) {
            reasoner = ReasonerRegistry.getOWLReasoner();
          } else {
            reasoner = new FBRuleReasoner(new ArrayList<Rule>());
          }
          model = ModelFactory2.createInfModel(reasoner, basemodel.getModel());
          if (rulesFile != null && !rulesFile.equals("")) {
            if (reasoner instanceof FBRuleReasoner) {
              ((FBRuleReasoner) reasoner).addRules(loadRules(rulesFile));
            } else if (reasoner instanceof RuleReasoner) {
              ((RuleReasoner) reasoner).setRules(loadRules(rulesFile));
            }
          }
          if (rules.size() > 0) {
            if (reasoner instanceof FBRuleReasoner) {
              ((FBRuleReasoner) reasoner).addRules(rules);
            } else if (reasoner instanceof RuleReasoner) {
              ((RuleReasoner) reasoner).setRules(rules);
            }
          }
          model.prepare();
          form.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              ReasoningComplete();
            }
          });
        } catch (final Exception e) {
          form.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              ErrorOccurred(e.toString());
            }
          });
        }
      }
    });
  }

  /**
   * Get statements as a list of triples from the knowledge base. Each argument can either be false
   * or a string. False is treated as a wildcard. Strings are interpreted as URIs.
   *
   * @param subject the subject to filter by
   * @param predicate the predicate to filter by
   * @param object the object to filter by
   * @return a list of triples matching the (subject, predicate, object) pattern
   */
  @SimpleFunction
  public List<List<String>> GetStatements(Object subject, Object predicate, Object object) {
    List<List<String>> result = new ArrayList<>();
    Resource s = null;
    Property p = null;
    RDFNode o = null;
    if (subject != Boolean.FALSE) {
      s = ResourceFactory.createResource(subject.toString());
    }
    if (predicate != Boolean.FALSE) {
      if (predicate.toString().equals("a")) {
        p = RDF.type;
      } else {
        p = ResourceFactory.createProperty(predicate.toString());
      }
    }
    if (object != Boolean.FALSE) {
      String ostr = object.toString();
      if (ostr.startsWith("http:") || ostr.startsWith("https://") || ostr.startsWith("file://")) {
        o = ResourceFactory.createResource(ostr);
      } else {
        o = ResourceFactory.createPlainLiteral(ostr);
      }
    }
    for (StmtIterator it = model.listStatements(s, p, o); it.hasNext(); ) {
      Statement st = it.next();
      result.add(Arrays.asList(
          st.getSubject().toString(),
          st.getPredicate().toString(),
          st.getObject().toString()));
    }
    return result;
  }

  /**
   * Populate the rule-based reasoner with the given ruleset of custom rules.
   *
   * @param ruleset the ruleset as a string
   */
  @SimpleFunction
  public void RulesFromRuleset(String ruleset) {
    rules = Rule.parseRules(ruleset);
  }

  /**
   * Synchronously evaluate a SPARQL query over the knowledge base. The return type depends on the
   * type of query run. For SELECT queries, the return value is a JSON-like dictionary containing
   * results in the SPARQL 1.1 Query Result format.
   *
   * @param query a string containing a valid SPARQL query
   * @return the query results
   */
  @SimpleFunction
  public Object Query(String query) {
    Query sparql = QueryFactory.create(query);
    QueryExecution qe = QueryExecutionFactory.create(sparql, model);
    if (sparql.isSelectType()) {
      ResultSet rs = qe.execSelect();
      YailDictionary result = new YailDictionary();
      YailDictionary head = new YailDictionary();
      head.put("vars", YailList.makeList(rs.getResultVars()));
      result.put("head", head);
      List<YailDictionary> results = new ArrayList<>();
      while (rs.hasNext()) {
        YailDictionary yailBinding = new YailDictionary();
        QuerySolution binding = rs.next();
        Iterator<String> vars = binding.varNames();
        while (vars.hasNext()) {
          String var = vars.next();
          RDFNode value = binding.get(var);
          if (value != null) {
            yailBinding.put(var, value.toString());
          }
        }
        results.add(yailBinding);
      }
      result.put("results", YailList.makeList(results));
      return result;
    }
    return Collections.emptyList();
  }

  ///endregion
  ///region Events

  /**
   * Runs when the reasoner has been prepared and any forward-chaining rules have finished.
   */
  @SimpleEvent
  public void ReasoningComplete() {
    EventDispatcher.dispatchEvent(this, "ReasoningComplete");
  }

  /**
   * Runs when the reasoner encounters an error during reasoning.
   *
   * @param message the error message
   */
  @SimpleEvent
  public void ErrorOccurred(String message) {
    if (!EventDispatcher.dispatchEvent(this, "ErrorOccurred", message)) {
      form.dispatchErrorOccurredEvent(this, "Run", ErrorMessages.ERROR_REASONER_FAILED, message);
    }
  }

  ///endregion

  private List<Rule> loadRules(String filename) throws IOException {
    InputStream in = null;
    try {
      in = MediaUtil.openMedia(form, filename);
      return Rule.parseRules(IOUtils.readStream(in));
    } finally {
      IOUtils.closeQuietly(LOG_TAG, in);
    }
  }
}