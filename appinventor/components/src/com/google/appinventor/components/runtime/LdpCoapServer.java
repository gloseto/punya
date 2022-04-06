// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2020 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0
// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2020 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.errors.PermissionException;
import com.google.appinventor.components.runtime.util.BulkPermissionRequest;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.FileUtil;
import android.Manifest;
import android.os.Environment;
import android.util.Log;

import java.net.SocketException;

import it.poliba.sisinflab.coap.ldp.exception.CoAPLDPException;
import it.poliba.sisinflab.coap.ldp.resources.CoAPLDPBasicContainer;
import it.poliba.sisinflab.coap.ldp.resources.CoAPLDPDirectContainer;
import it.poliba.sisinflab.coap.ldp.resources.CoAPLDPIndirectContainer;
import it.poliba.sisinflab.coap.ldp.resources.CoAPLDPNonRDFSource;
import it.poliba.sisinflab.coap.ldp.resources.CoAPLDPRDFSource;
import it.poliba.sisinflab.coap.ldp.server.CoAPLDPServer;
import it.poliba.sisinflab.rdf.vocabulary.SSN_XG;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.rdf4j.rio.RDFFormat;



import java.io.IOException;

import com.google.appinventor.components.annotations.UsesLibraries;

@DesignerComponent(version = YaVersion.SOUND_RECORDER_COMPONENT_VERSION,
        description = "<p>LDP-COAP Server</p>",
        category = ComponentCategory.CONNECTIVITY,
        nonVisible = true,
        iconName = "images/ldpcoapclient.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.INTERNET," +
        "android.permission.WRITE_EXTERNAL_STORAGE," +
        "android.permission.READ_EXTERNAL_STORAGE")

@UsesLibraries(libraries= "ldp-coap-core-1.2.0-SNAPSHOT.jar," +
"element-connector-1.0.7.jar,"+
"marmotta-util-rdfpatch-rdf4j-3.3.0.jar,"+
"californium-core-ldp-1.0.0-SNAPSHOT.jar,"+
"slf4j-android-1.7.30.jar,"+
"guava-14.0.1.jar")

public final class LdpCoapServer extends AndroidNonvisibleComponent
        implements Component {

  private static final String TAG = "LdpCoapServer";
  protected boolean STATUS = false;
  protected String BASE_URI;
  static CoAPLDPServer server = null;
  static CoAPLDPBasicContainer bc = null;
  static CoAPLDPDirectContainer dc = null;
  static CoAPLDPIndirectContainer ic = null;


  public LdpCoapServer(final ComponentContainer container) {super(container.$form());
  }

  @SimpleProperty(
          description = "Set Server URI and instantiate it",
          category = PropertyCategory.BEHAVIOR)
  public void BASE_URI(String URI) {
    BASE_URI = URI;
    server = new CoAPLDPServer(BASE_URI);
    server.addHandledNamespace(SSN_XG.PREFIX, SSN_XG.NAMESPACE + "#");
    /*** Handle read-only properties ***/
    server.setReadOnlyProperty("http://purl.org/dc/terms/created");
    server.setConstrainedByURI("http://sisinflab.poliba.it/swottools/ldp-coap/ldp-report.html");
    /*** Handle not persisted properties ***/
    server.setNotPersistedProperty("http://example.com/ns#comment");
  }
  @SimpleProperty(
          description = "Get Server BASE URI",
          category = PropertyCategory.BEHAVIOR)
  public String BASE_URI() {
    return BASE_URI;
  }
  @SimpleProperty(
          description = "Get Status",
          category = PropertyCategory.BEHAVIOR)
  public String STATUS() {
    if (STATUS == true) {
      return "ALIVE";
    } else {
      return "DEAD";
    }
  }


  @SimpleFunction(
          description = "Start Server")
  public void ServerStart() {
    if((!STATUS)&&(server!=null)) {
      server.start();
      STATUS = true;
    }
  }
  @SimpleFunction(
          description = "Stop Server")
  public void ServerStop() {
    if((STATUS)&&(server!=null)) {
      server.stop();
      STATUS = false;
    }
  }
  @SimpleFunction(
          description = "Destroy Server")
  public void ServerDestroy() {
    if((STATUS)&&(server!=null)) {
      server.destroy();
      STATUS = false;
      server = null;
    }
  }
  @SimpleFunction(
          description = "Create Basic Container")
  public void createBasicContainer(String ldp_dc_name,String rdf_title,String rdf_description) {
    /*** Add LDP-BasicContainer ***/
    bc =  server.createBasicContainer(ldp_dc_name);
    bc.setRDFTitle(rdf_title);
    bc.setRDFDescription(rdf_description);
  }
  @SimpleFunction(
          description = "Create Basic Container in a Basic Container")
  public void createBasicContainerInBasicContainer(String ldp_dc_name,String rdf_title,String rdf_description) {
    /*** Add LDP-BasicContainer ***/
    if(bc!=null) {
      bc = bc.createBasicContainer(ldp_dc_name);
      bc.setRDFTitle(rdf_title);
      bc.setRDFDescription(rdf_description);
    }
  }
  @SimpleFunction(
          description = "Create Basic Container RDF Resource")
  public void createBasicContainerRDFSource(String name,String type, String title, String description) {
    /*** Add LDP-BasicContainer ***/
    if(bc!=null) {
      CoAPLDPRDFSource resource = bc.createRDFSource(name, type);
      resource.setRDFTitle(title);
      resource.setRDFDescription(description);
    }
  }
  @SimpleFunction(
          description = "Create Basic Container Non RDF Resource")
  public void createBasicContainerNonRDFSource(String Name,String Title) {
    /*** Add LDP-RDFSource ***/
    if(bc!=null) {
      CoAPLDPRDFSource src = bc.createRDFSource(Name);
      src.setRDFTitle(Title);
    }
  }
@SimpleFunction(
          description = "Add Basic Container accept Post type")
  public void addBasicContainerAcceptPostType(int type) {
    if(bc!=null) {
     bc.addAcceptPostType(type);
    }
  }
  @SimpleFunction(
          description = "Create Direct Container")
  public void createDirectContainer(String ldp_dc_name, String member_res_name, String member_res_type, String membershipRelation, String isMemberOfRelation, String title, String memberTitle) {
    /*** Add LDP-DirectContainer ***/
    if(isMemberOfRelation=="null"){
      isMemberOfRelation = null;
    }
    if(membershipRelation=="null"){
      membershipRelation = null;
    }
    dc = server.createDirectContainer(ldp_dc_name, member_res_name, member_res_type, membershipRelation, isMemberOfRelation);
    dc.setRDFTitle(title);
    dc.getMemberResource().setRDFTitle(memberTitle);
  }
  @SimpleFunction(
          description = "Create Direct Container in a Basic Container")
  public void createDirectContainerInBasicContainer(String ldp_dc_name, String member_res_name, String member_res_type, String membershipRelation, String isMemberOfRelation, String title, String memberTitle) {
    /*** Add LDP-DirectContainer ***/
    if(bc!=null) {
      if(isMemberOfRelation=="null"){
        isMemberOfRelation = null;
      }
      if(membershipRelation=="null"){
        membershipRelation = null;
      }
      dc = bc.createDirectContainer(ldp_dc_name, member_res_name, member_res_type, membershipRelation, isMemberOfRelation);
      dc.setRDFTitle(title);
      dc.getMemberResource().setRDFTitle(memberTitle);
    }
  }
  @SimpleFunction(
          description = "Create Direct Container RDFSource")
  public void createDirectContainerRDFSource(String name,String type, String title, String description) {
    /*** Add LDP-DirectContainer RDFSource ***/
    if(dc!=null) {
      CoAPLDPRDFSource resDC = dc.createRDFSource(name, type);
      resDC.setRDFTitle(title);
      resDC.setRDFDescription(description);

    }
  }
  @SimpleFunction(
          description = "Create Direct Container Non RDF Resource")
  public void createDirectContainerNonRDFSource(String Name,String Title) {
    /*** Add LDP-RDFSource ***/
    if(dc!=null) {
      CoAPLDPRDFSource src = dc.createRDFSource(Name);
      src.setRDFTitle(Title);
    }
  }
  @SimpleFunction(
          description = "Add Direct Container accept Post type")
  public void addDirectContainerAcceptPostType(int type) {
    if(dc!=null) {
     dc.addAcceptPostType(type);
    }
  }

  @SimpleFunction(
          description = "Create Indirect Container")
  public void createIndirectContainer(String ldp_ic_name, String member_res_name, String member_res_type, String membershipRelation, String insertedContentRelation, String title, String memberTitle) {
    /*** Add LDP-DirectContainer ***/
    ic = server.createIndirectContainer(ldp_ic_name, member_res_name, member_res_type, membershipRelation, insertedContentRelation);
    ic.setRDFTitle(title);
    ic.getMemberResource().setRDFTitle(memberTitle);
  }
  @SimpleFunction(
          description = "Create Indirect Container in a Basic Container")
  public void createIndirectContainerInBasicContainer(String ldp_ic_name, String member_res_name, String member_res_type, String membershipRelation, String insertedContentRelation, String title, String memberTitle) {
    /*** Add LDP-DirectContainer ***/
    ic = bc.createIndirectContainer(ldp_ic_name, member_res_name, member_res_type, membershipRelation, insertedContentRelation);
    ic.setRDFTitle(title);
    ic.getMemberResource().setRDFTitle(memberTitle);
  }
  @SimpleFunction(description = "Create Indirect Container RDFSource With Derived URI")
  public void createIndirectContainerRDFSourceWithDerivedUri(String name,String type, String member_derived_uri, String title) {
  /*** Add LDP-RS with Derived URI to the LDP-IC ***/
    CoAPLDPRDFSource subSystem = ic.createRDFSourceWithDerivedURI(name, type, BASE_URI + member_derived_uri);
    subSystem.setRDFTitle(title);
  }
  @SimpleFunction(
          description = "Add Indirect Container accept Post type")
  public void addIndirectContainerAcceptPostType(int type) {
    if(ic!=null) {
     ic.addAcceptPostType(type);
    }
  }

  @SimpleFunction(
          description = "Create LDP-NonRDF Resource in root location")
  public void createRootLDPNonRDFSource(String name,String data, int type) {
    /*** Add LDP-NonRDFSource ***/
    CoAPLDPNonRDFSource nr = server.createNonRDFSource(name, type);
    nr.setData((data).getBytes());
  }
  @SimpleFunction(
          description = "Create LDP-RDF Resource in root location")
  public void createRootLDPRDFSource(String name, String type, String title) {
    /*** Add LDP-RDFSource ***/
    CoAPLDPRDFSource src = server.createRDFSource(name, type);
    src.setRDFTitle(title);
  }

  @SimpleFunction(description = "text/plain code")
  public int TextPlain() {
    return MediaTypeRegistry.TEXT_PLAIN;
  }
  @SimpleFunction(description = "text/csv code")
  public int TextCsv() {
    return MediaTypeRegistry.TEXT_CSV;
  }
  @SimpleFunction(description = "text/html code")
  public int TextHtml() {
    return MediaTypeRegistry.TEXT_HTML;
  }
  @SimpleFunction(description = "image/gif code")
  public int ImageGif() {
    return MediaTypeRegistry.IMAGE_GIF;
  }
  @SimpleFunction(description = "image/jpeg code")
  public int ImageJpeg() {
    return MediaTypeRegistry.IMAGE_JPEG;
  }
  @SimpleFunction(description = "image/png code")
  public int ImagePng() {
    return MediaTypeRegistry.IMAGE_PNG;
  }
  @SimpleFunction(description = "image/tiff code")
  public int ImageTiff() {
    return MediaTypeRegistry.IMAGE_TIFF;
  }
  @SimpleFunction(description = "application/link-format code")
  public int ApplicationLinkFormat() {
    return MediaTypeRegistry.APPLICATION_LINK_FORMAT;
  }
  @SimpleFunction(description = "application/xml code")
  public int ApplicationXml() {
    return MediaTypeRegistry.APPLICATION_XML;
  }
  @SimpleFunction(description = "application/octet-stream code")
  public int ApplicationOctetStream() {
    return MediaTypeRegistry.APPLICATION_OCTET_STREAM;
  }
  @SimpleFunction(description = "application/rdf+xml code")
  public int ApplicationRdfXml() {
    return MediaTypeRegistry.APPLICATION_RDF_XML;
  }
  @SimpleFunction(description = "application/soap+xml code")
  public int ApplicationSoapXml() {
    return MediaTypeRegistry.APPLICATION_SOAP_XML;
  }
  @SimpleFunction(description = "application/atom+xml code")
  public int ApplicationAtomXml() {
    return MediaTypeRegistry.APPLICATION_ATOM_XML;
  }
  @SimpleFunction(description = "application/xmpp+xml code")
  public int ApplicationXmppXml() {
    return MediaTypeRegistry.APPLICATION_XMPP_XML;
  }
  @SimpleFunction(description = "application/exi code")
  public int ApplicationExi() {
    return MediaTypeRegistry.APPLICATION_EXI;
  }
  @SimpleFunction(description = "application/fastinfoset code")
  public int ApplicationFastinfoset() {
    return MediaTypeRegistry.APPLICATION_FASTINFOSET;
  }
  @SimpleFunction(description = "application/soap+fastinfoset code")
  public int ApplicationSoapFastinfoset() {
    return MediaTypeRegistry.APPLICATION_SOAP_FASTINFOSET;
  }
  @SimpleFunction(description = "application/json code")
  public int ApplicationJson() {
    return MediaTypeRegistry.APPLICATION_JSON;
  }
  @SimpleFunction(description = "application/x-obix-binary code")
  public int ApplicationXobixBinary() {
    return MediaTypeRegistry.APPLICATION_X_OBIX_BINARY;
  }
  @SimpleFunction(description = "text/turtle code")
  public int TextTurtle() {
    return MediaTypeRegistry.TEXT_TURTLE;
  }
  @SimpleFunction(description = "application/ld+json code")
  public int ApplicationLdJson() {
    return MediaTypeRegistry.APPLICATION_LD_JSON;
  }
  @SimpleFunction(description = "application/rdf-patch code")
  public int ApplicationRdfPatch() {
    return MediaTypeRegistry.APPLICATION_RDF_PATCH;
  }
  @SimpleFunction(description = "application/gzip code")
  public int ApplicationGzip() {
    return MediaTypeRegistry.APPLICATION_GZIP;
  }
  @SimpleFunction(description = "application/bz2 code")
  public int ApplicationBzip2() {
    return MediaTypeRegistry.APPLICATION_BZIP2;
  }
  @SimpleFunction(description = "application/bson code")
  public int ApplicationBson() {
    return MediaTypeRegistry.APPLICATION_BSON;
  }
  @SimpleFunction(description = "application/ubjson code")
  public int ApplicationUbjson() {
    return MediaTypeRegistry.APPLICATION_UBJSON;
  }
  @SimpleFunction(description = "application/msgpack code")
  public int ApplicationMsgpack() {
    return MediaTypeRegistry.APPLICATION_MSGPACK;
  }

}
