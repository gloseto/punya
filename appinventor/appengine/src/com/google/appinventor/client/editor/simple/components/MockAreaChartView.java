// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2019-2020 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.editor.simple.components;

/**
 * Chart View for the Area Chart
 *
 * Responsible for the GUI of the Area Chart.
 */
public class MockAreaChartView extends MockLineChartViewBase {
  /**
   * Creates a new Mock Area Chart view instance.
   */
  public MockAreaChartView() {
    super();
  }

  @Override
  protected void initializeDefaultSettings() {
    super.initializeDefaultSettings();

    // Due to differing drawing orders of the Android and the
    // Mock Chart implementations, the Legend has to be reversed
    // in the Mock Area Chart and the Data Series have to be added
    // in reverse order as well to represent the Android data series
    // accurately.
    chartWidget.getOptions().getLegend().setReverse(true);
  }

  @Override
  public MockChartDataModel createDataModel() {
    return new MockAreaChartDataModel(this);
  }
}
