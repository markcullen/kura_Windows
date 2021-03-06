/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.Network;

import java.util.ArrayList;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;
import org.eclipse.kura.web.shared.model.GwtSession;
import org.eclipse.kura.web.shared.service.GwtNetworkService;
import org.eclipse.kura.web.shared.service.GwtNetworkServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.gwt.DataGrid;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

public class NetworkInterfacesTableUi extends Composite {

	private static NetworkInterfacesTableUiUiBinder uiBinder = GWT.create(NetworkInterfacesTableUiUiBinder.class);

	interface NetworkInterfacesTableUiUiBinder extends
			UiBinder<Widget, NetworkInterfacesTableUi> {
	}

	private static final Messages MSGS = GWT.create(Messages.class);
	private final GwtNetworkServiceAsync gwtNetworkService = GWT.create(GwtNetworkService.class);
	
	GwtSession session;
	NetworkTabsUi tabs;
	GwtNetInterfaceConfig selection;

	@UiField
	Alert notification;
	@UiField
	
	DataGrid<GwtNetInterfaceConfig> interfacesGrid = new DataGrid<GwtNetInterfaceConfig>();
	
	private ListDataProvider<GwtNetInterfaceConfig> interfacesProvider = new ListDataProvider<GwtNetInterfaceConfig>();
	final SingleSelectionModel<GwtNetInterfaceConfig> selectionModel = new SingleSelectionModel<GwtNetInterfaceConfig>();

	public NetworkInterfacesTableUi(GwtSession session,	NetworkTabsUi tabsPanel) {
		initWidget(uiBinder.createAndBindUi(this));
		this.session = session;
		this.tabs = tabsPanel;
		initTable();
		loadData();

		selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

					@Override
					public void onSelectionChange(SelectionChangeEvent event) {
						if (selection != null && tabs.isDirty()) {
							// there was an earlier selection, changes have not
							// been saved						
							final Modal confirm = new Modal();
							ModalBody confirmBody = new ModalBody();
							ModalFooter confirmFooter = new ModalFooter();

							confirm.setTitle(MSGS.confirm());
							confirmBody.add(new Span(MSGS.deviceConfigDirty()));
							confirmFooter.add(new Button(MSGS.yesButton(),
									new ClickHandler() {
										@Override
										public void onClick(ClickEvent event) {
											confirm.hide();
											selection = selectionModel.getSelectedObject();
											if (selection != null) {
												tabs.setNetInterface(selection);
												tabs.setDirty(false);
											}
										}
									}));

							confirmFooter.add(new Button(MSGS.noButton(),
									new ClickHandler() {
										@Override
										public void onClick(ClickEvent event) {
											confirm.hide();
										}
									}));
							confirm.add(confirmBody);
							confirm.add(confirmFooter);
							confirm.show();

						} else {
							// no unsaved changes
							selection = selectionModel.getSelectedObject();
							if (selection != null) {
								tabs.setNetInterface(selection);
							}
						}
					}
				});

	}

	
	public void refresh() {
		if (selection != null && tabs.isDirty()) {
			// there was an earlier selection, changes have not been saved
			final Modal confirm = new Modal();
			ModalBody confirmBody = new ModalBody();
			ModalFooter confirmFooter = new ModalFooter();

			confirm.setTitle(MSGS.confirm());
			confirmBody.add(new Span(MSGS.deviceConfigDirty()));
			confirmFooter.add(new Button(MSGS.yesButton(),
					new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							confirm.hide();
							//selection = null;
							tabs.setDirty(false);
							loadData();
						}
					}));

			confirmFooter.add(new Button(MSGS.noButton(),
					new ClickHandler() {
						@Override
						public void onClick(ClickEvent event) {
							confirm.hide();
						}
					}));
			confirm.add(confirmBody);
			confirm.add(confirmFooter);
			confirm.show();
		} else {
			tabs.setDirty(false);
			loadData();
		}
	}
	
	
	
	/*--------------------------------------
	 * -------Private methods---------------
	 --------------------------------------*/
	
	private void initTable() {
		TextColumn<GwtNetInterfaceConfig> col1 = new TextColumn<GwtNetInterfaceConfig>() {
			@Override
			public String getValue(GwtNetInterfaceConfig object) {
				return object.getName();
			}
		};
		col1.setCellStyleNames("status-table-row");
		interfacesGrid.addColumn(col1, MSGS.netInterfaceName());

		interfacesProvider.addDataDisplay(interfacesGrid);
		interfacesGrid.setSelectionModel(selectionModel);

	}

	private void loadData() {
		EntryClassUi.showWaitModal();
		interfacesProvider.getList().clear();
		gwtNetworkService.findNetInterfaceConfigurations(new AsyncCallback<ArrayList<GwtNetInterfaceConfig>>() {
					
					@Override
					public void onFailure(Throwable caught) {
						EntryClassUi.hideWaitModal();
						FailureHandler.handle(caught);
					}

					@Override
					public void onSuccess(ArrayList<GwtNetInterfaceConfig> result) {
						for (GwtNetInterfaceConfig pair : result) {
							interfacesProvider.getList().add(pair);
						}
						
						interfacesProvider.flush();
						
						if (interfacesProvider.getList().size() > 0) {
							interfacesGrid.setVisible(true);
							notification.setVisible(false);
							selectionModel.setSelected(interfacesProvider.getList().get(0),	true);
							interfacesGrid.getSelectionModel().setSelected(interfacesProvider.getList().get(0), true);

						} else {
							interfacesGrid.setVisible(false);
							notification.setVisible(true);
							notification.setText(MSGS.netTableNoInterfaces());
						}
						EntryClassUi.hideWaitModal();
					}
				});
	}

}
