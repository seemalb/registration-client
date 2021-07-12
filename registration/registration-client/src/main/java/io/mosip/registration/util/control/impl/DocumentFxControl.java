package io.mosip.registration.util.control.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;


import io.mosip.registration.dto.mastersync.GenericDto;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.registration.api.docscanner.DocScannerUtil;
import io.mosip.registration.audit.AuditManagerService;
import io.mosip.registration.config.AppConfig;
import io.mosip.registration.constants.AuditEvent;
import io.mosip.registration.constants.AuditReferenceIdTypes;
import io.mosip.registration.constants.Components;
import io.mosip.registration.constants.RegistrationConstants;
import io.mosip.registration.constants.RegistrationUIConstants;
import io.mosip.registration.context.ApplicationContext;
import io.mosip.registration.context.SessionContext;
import io.mosip.registration.controller.FXUtils;
import io.mosip.registration.controller.Initialization;
import io.mosip.registration.controller.reg.DocumentScanController;
import io.mosip.registration.dto.schema.UiSchemaDTO;
import io.mosip.registration.dto.mastersync.DocumentCategoryDto;
import io.mosip.registration.dto.packetmanager.DocumentDto;
import io.mosip.registration.entity.DocumentType;
import io.mosip.registration.service.sync.MasterSyncService;
import io.mosip.registration.util.common.ComboBoxAutoComplete;
import io.mosip.registration.util.control.FxControl;
import io.mosip.registration.validator.RequiredFieldValidator;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class DocumentFxControl extends FxControl {

	/**
	 * Instance of {@link Logger}
	 */
	private static final Logger LOGGER = AppConfig.getLogger(DocumentFxControl.class);

	private static String loggerClassName = " Text Field Control Type Class";

	private DocumentScanController documentScanController;

	private MasterSyncService masterSyncService;

	private String PREVIEW_ICON = "previewIcon";

	private String CLEAR_ID = "clear";

	public DocumentFxControl() {
		org.springframework.context.ApplicationContext applicationContext = Initialization.getApplicationContext();
		auditFactory = applicationContext.getBean(AuditManagerService.class);
		documentScanController = applicationContext.getBean(DocumentScanController.class);
		masterSyncService = applicationContext.getBean(MasterSyncService.class);
	}

	@Override
	public FxControl build(UiSchemaDTO uiSchemaDTO) {
		this.uiSchemaDTO = uiSchemaDTO;
		this.control = this;

		HBox hBox = new HBox();
		hBox.setSpacing(20);

		// DROP-DOWN
		hBox.getChildren().add(create(uiSchemaDTO));

		// REF-FIELD
		hBox.getChildren().add(createDocRef(uiSchemaDTO.getId()));

		// CLEAR IMAGE
		GridPane tickMarkGridPane = getImageGridPane(PREVIEW_ICON, RegistrationConstants.DOC_PREVIEW_ICON);
		tickMarkGridPane.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {

			scanDocument((ComboBox<DocumentCategoryDto>) getField(uiSchemaDTO.getId()), uiSchemaDTO.getSubType(), true);

		});
		// TICK-MARK
		hBox.getChildren().add(tickMarkGridPane);

		// CLEAR IMAGE
		GridPane clearGridPane = getImageGridPane(CLEAR_ID, RegistrationConstants.CLOSE_IMAGE_PATH);
		clearGridPane.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
			ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(
					uiSchemaDTO.getId());
			comboBox.getSelectionModel().clearSelection();
			clearCapturedDocuments();
		});
		hBox.getChildren().add(clearGridPane);

		// SCAN-BUTTON
		hBox.getChildren().add(createScanButton(uiSchemaDTO));

		this.node = hBox;

		setListener(getField(uiSchemaDTO.getId() + RegistrationConstants.BUTTON));

		changeNodeOrientation(hBox, getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));

		fillData(masterSyncService.getDocumentCategories(uiSchemaDTO.getSubType(),
				getRegistrationDTo().getSelectedLanguagesByApplicant().get(0)));

		return this.control;
	}

	private void clearCapturedDocuments() {
		AuditEvent auditEvent = null;
		try {
			auditEvent = AuditEvent.valueOf(String.format("REG_DOC_%S_DELETE", uiSchemaDTO.getSubType()));
		} catch (Exception exception) {
			LOGGER.error("Unable to find audit event for button : " + uiSchemaDTO.getSubType());

			auditEvent = AuditEvent.REG_DOC_DELETE;
		}
		auditFactory.audit(auditEvent, Components.REG_DOCUMENTS, SessionContext.userId(),
				AuditReferenceIdTypes.USER_ID.getReferenceTypeId());
		
		getRegistrationDTo().getDocuments().remove(this.uiSchemaDTO.getId());
		
		TextField textField = (TextField) getField(
				uiSchemaDTO.getId() + RegistrationConstants.DOC_TEXT_FIELD);
		textField.setText(RegistrationConstants.EMPTY);
		
		getField(uiSchemaDTO.getId() + PREVIEW_ICON).setVisible(false);
		getField(uiSchemaDTO.getId() + CLEAR_ID).setVisible(false);
		getField(uiSchemaDTO.getId() + PREVIEW_ICON).setManaged(true);
		getField(uiSchemaDTO.getId() + CLEAR_ID).setManaged(true);
	}

	private GridPane getImageGridPane(String id, String imagePath) {
		VBox imageVBox = new VBox();
		imageVBox.setId(uiSchemaDTO.getId() + id);
		ImageView imageView = new ImageView(
				(new Image(this.getClass().getResourceAsStream(imagePath), 25, 25, true, true)));

		boolean isVisible = getData() != null ? true : false;
		imageView.setPreserveRatio(true);
		imageVBox.setVisible(isVisible);

		imageVBox.getChildren().add(imageView);

		GridPane gridPane = new GridPane();
		RowConstraints rowConstraint1 = new RowConstraints();
		RowConstraints rowConstraint2 = new RowConstraints();
		rowConstraint1.setPercentHeight(45);
		rowConstraint2.setPercentHeight(55);
		gridPane.getRowConstraints().addAll(rowConstraint1, rowConstraint2);
		gridPane.add(imageVBox, 0, 1);

		return gridPane;
	}

	private GridPane createScanButton(UiSchemaDTO uiSchemaDTO) {

		Button scanButton = new Button();
		scanButton.setText(ApplicationContext.getInstance()
				.getBundle(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0), RegistrationConstants.LABELS)
				.getString(RegistrationConstants.SCAN_BUTTON));
		scanButton.setId(uiSchemaDTO.getId() + RegistrationConstants.BUTTON);
		scanButton.getStyleClass().add(RegistrationConstants.DOCUMENT_CONTENT_BUTTON);
		scanButton.setGraphic(new ImageView(
				new Image(this.getClass().getResourceAsStream(RegistrationConstants.SCAN), 12, 12, true, true)));

		GridPane scanButtonGridPane = new GridPane();
		RowConstraints rowConstraint1 = new RowConstraints();
		RowConstraints rowConstraint2 = new RowConstraints();
		rowConstraint1.setPercentHeight(35);
		rowConstraint2.setPercentHeight(65);
		scanButtonGridPane.getRowConstraints().addAll(rowConstraint1, rowConstraint2);
		scanButtonGridPane.setPrefWidth(80);
		scanButtonGridPane.add(scanButton, 0, 1);

		return scanButtonGridPane;
	}

	private void scanDocument(ComboBox<DocumentCategoryDto> comboBox, String subType, boolean isPreviewOnly) {

		if (isValid()) {
			documentScanController.setFxControl(this);
			documentScanController.scanDocument(uiSchemaDTO.getId(), comboBox.getValue().getCode(),	isPreviewOnly);

		} else {
			documentScanController.generateAlert(RegistrationConstants.ERROR, RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.PLEASE_SELECT)
					+ RegistrationConstants.SPACE + uiSchemaDTO.getSubType() + " " + RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.DOCUMENT));
		}

	}

	private VBox createDocRef(String id) {
		/** Container holds title, fields and validation message elements */
		VBox simpleTypeVBox = new VBox();
		simpleTypeVBox.setId(id + RegistrationConstants.DOC_TEXT_FIELD + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);
		simpleTypeVBox.getStyleClass().add(RegistrationConstants.SCAN_VBOX);

		double prefWidth = simpleTypeVBox.getPrefWidth();

		List<String> labels = new ArrayList<>();
		getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(lCode -> {

			ResourceBundle rb = ApplicationContext.getInstance().getBundle(lCode, RegistrationConstants.LABELS);
			labels.add(rb.getString(RegistrationConstants.REF_NUMBER));
		});

		String titleText = String.join(RegistrationConstants.SLASH, labels);
		ResourceBundle rb = ApplicationContext.getInstance()
				.getBundle(getRegistrationDTo().getSelectedLanguagesByApplicant().get(0), RegistrationConstants.LABELS);

		/** Title label */
		Label fieldTitle = getLabel(id + RegistrationConstants.DOC_TEXT_FIELD + RegistrationConstants.LABEL, titleText,
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, false, prefWidth);

		simpleTypeVBox.getChildren().add(fieldTitle);

		/** Text Field */
		TextField textField = getTextField(id + RegistrationConstants.DOC_TEXT_FIELD, titleText,
				RegistrationConstants.DEMOGRAPHIC_TEXTFIELD, prefWidth, false);

		textField.textProperty().addListener((observable, oldValue, newValue) -> {

			Label label = (Label) getField(
					uiSchemaDTO.getId() + RegistrationConstants.DOC_TEXT_FIELD + RegistrationConstants.LABEL);
			if (textField.getText().isEmpty()) {
				label.setVisible(false);
			} else {

				label.setVisible(true);
			}
		});

		simpleTypeVBox.getChildren().add(textField);
		return simpleTypeVBox;
	}

	private VBox create(UiSchemaDTO uiSchemaDTO) {

		String fieldName = uiSchemaDTO.getId();

		/** Container holds title, fields and validation message elements */
		VBox simpleTypeVBox = new VBox();
		simpleTypeVBox.setId(fieldName + RegistrationConstants.VBOX);
		simpleTypeVBox.setSpacing(5);

		double prefWidth = 300;

		List<String> labels = new ArrayList<>();
		getRegistrationDTo().getSelectedLanguagesByApplicant().forEach(lCode -> {
			labels.add(this.uiSchemaDTO.getLabel().get(lCode));
		});
		String titleText = String.join(RegistrationConstants.SLASH, labels) + getMandatorySuffix(uiSchemaDTO);

		/** Title label */
		Label fieldTitle = getLabel(fieldName + RegistrationConstants.LABEL, titleText,
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, false, prefWidth);
		simpleTypeVBox.getChildren().add(fieldTitle);

		/** comboBox Field */
		ComboBox<DocumentCategoryDto> comboBox = getComboBox(fieldName, titleText,
				RegistrationConstants.DEMOGRAPHIC_TEXTFIELD, prefWidth, false);
		simpleTypeVBox.getChildren().add(comboBox);

		comboBox.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
			if (comboBox.getSelectionModel().getSelectedItem() != null) {
				clearCapturedDocuments();
				List<String> toolTipTextList = new ArrayList<>();
				String selectedCode = comboBox.getSelectionModel().getSelectedItem().getCode();
				for (String langCode : getRegistrationDTo().getSelectedLanguagesByApplicant()) {
					DocumentType documentType = masterSyncService.getDocumentType(selectedCode, langCode);
					if (documentType != null) {
						toolTipTextList.add(documentType.getName());
					}
				}
				Label messageLabel = (Label) getField(uiSchemaDTO.getId() + RegistrationConstants.MESSAGE);
				messageLabel.setText(String.join(RegistrationConstants.SLASH, toolTipTextList));
				fieldTitle.setVisible(true);
			} else {
				Label messageLabel = (Label) getField(uiSchemaDTO.getId() + RegistrationConstants.MESSAGE);
				messageLabel.setText(RegistrationConstants.EMPTY);
			}
		});

		comboBox.setOnMouseExited(event -> {
			if (comboBox.getTooltip() != null) {
				comboBox.getTooltip().hide();
			}

			Label messageLabel = (Label) getField(uiSchemaDTO.getId() + RegistrationConstants.MESSAGE);
			messageLabel.setVisible(false);
			messageLabel.setManaged(false);
		});

		comboBox.setOnMouseEntered((event -> {
			Label messageLabel = (Label) getField(uiSchemaDTO.getId() + RegistrationConstants.MESSAGE);
			if (messageLabel.getText()!=null && !messageLabel.getText().isEmpty()) {
				messageLabel.setVisible(true);
				messageLabel.setManaged(true);
			}
		}));

		Label messageLabel = getLabel(uiSchemaDTO.getId() + RegistrationConstants.MESSAGE, null,
				RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL, false, simpleTypeVBox.getPrefWidth());
		messageLabel.setWrapText(true);
		messageLabel.setPrefWidth(prefWidth);
		messageLabel.setManaged(false);
		simpleTypeVBox.getChildren().add(messageLabel);

		return simpleTypeVBox;
	}

	@Override
	public void setData(Object data) {

		try {

			if (data == null) {
				getField(uiSchemaDTO.getId() + PREVIEW_ICON).setVisible(false);
				getField(uiSchemaDTO.getId() + CLEAR_ID).setVisible(false);
			} else {
				List<BufferedImage> bufferedImages = (List<BufferedImage>) data;
				if (bufferedImages == null || bufferedImages.isEmpty()) {
					documentScanController.generateAlert(RegistrationConstants.ERROR,
							RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.SCAN_DOCUMENT_EMPTY));
					return;
				}

				String configuredDocType = ApplicationContext.getStringValueFromApplicationMap(RegistrationConstants.DOC_TYPE);
				byte[] byteArray =  ("pdf".equalsIgnoreCase(configuredDocType)) ?
						DocScannerUtil.asPDF(bufferedImages) : DocScannerUtil.asImage(bufferedImages);

				if (byteArray == null) {
					documentScanController.generateAlert(RegistrationConstants.ERROR,
							RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.SCAN_DOCUMENT_CONVERTION_ERR));
					return;
				}

				int docSize = Integer.parseInt(documentScanController
						.getValueFromApplicationContext(RegistrationConstants.DOC_SIZE)) / (1024 * 1024);
				if (docSize <= (byteArray.length / (1024 * 1024))) {
					bufferedImages.clear();
					documentScanController.generateAlert(RegistrationConstants.ERROR,
							RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.SCAN_DOC_SIZE).replace("1", Integer.toString(docSize)));
					return;
				}

				ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiSchemaDTO.getId());
				DocumentDto documentDto = getRegistrationDTo().getDocuments().get(uiSchemaDTO.getId());
				if (documentDto == null) {
					documentDto = new DocumentDto();
					documentDto.setFormat(configuredDocType);
					documentDto.setCategory(uiSchemaDTO.getSubType());
					documentDto.setOwner(RegistrationConstants.APPLICANT);
				}

				documentDto.setType(comboBox.getValue().getCode());
				documentDto.setValue(uiSchemaDTO.getSubType().concat(RegistrationConstants.UNDER_SCORE)
						.concat(comboBox.getValue().getCode()));

				documentDto.setDocument(byteArray);
				TextField textField = (TextField) getField(
						uiSchemaDTO.getId() + RegistrationConstants.DOC_TEXT_FIELD);
				documentDto.setRefNumber(textField.getText());
				getRegistrationDTo().addDocument(uiSchemaDTO.getId(), documentDto);

				getField(uiSchemaDTO.getId() + PREVIEW_ICON).setVisible(true);
				getField(uiSchemaDTO.getId() + CLEAR_ID).setVisible(true);

				getField(uiSchemaDTO.getId() + PREVIEW_ICON).setManaged(true);
				getField(uiSchemaDTO.getId() + CLEAR_ID).setManaged(true);

				Label label = (Label) getField(uiSchemaDTO.getId()+RegistrationConstants.LABEL);
				label.getStyleClass().clear();
				label.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_FIELD_LABEL);
			}
		} catch (IOException exception) {
			LOGGER.error("Unable to parse the buffered images to byte array ", exception);
			getField(uiSchemaDTO.getId() + PREVIEW_ICON).setVisible(false);
			getField(uiSchemaDTO.getId() + CLEAR_ID).setVisible(false);
			documentScanController.generateAlert(RegistrationConstants.ERROR,
					RegistrationUIConstants.getMessageLanguageSpecific(RegistrationUIConstants.UNABLE_LOAD_REG_PAGE));
		}
		refreshFields();
	}

	@Override
	public Object getData() {
		return documentScanController.getRegistrationDTOFromSession().getDocuments().get(uiSchemaDTO.getId());
	}

	@Override
	public boolean isValid() {
		String poeDocValue = documentScanController
				.getValueFromApplicationContext(RegistrationConstants.POE_DOCUMENT_VALUE);

		ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiSchemaDTO.getId());
		if (comboBox.getValue() == null) {
			comboBox.requestFocus();
			return false;
		} else if (comboBox.getValue().getCode().equalsIgnoreCase(poeDocValue)) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean isEmpty() {
		ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiSchemaDTO.getId());
		return (comboBox.getValue() == null);
	}

	@Override
	public List<GenericDto> getPossibleValues(String langCode) {

		return null;
	}

	@Override
	public void setListener(Node node) {
		Button scanButton = (Button) node;
		scanButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {

				AuditEvent auditEvent = null;
				try {
					auditEvent = AuditEvent
							.valueOf(String.format("REG_DOC_%S_SCAN", uiSchemaDTO.getSubType()));
				} catch (Exception exception) {
					auditEvent = AuditEvent.REG_DOC_SCAN;
				}
				auditFactory.audit(auditEvent, Components.REG_DOCUMENTS, SessionContext.userId(),
						AuditReferenceIdTypes.USER_ID.getReferenceTypeId());

				Button clickedBtn = (Button) event.getSource();
				clickedBtn.getId();
				// TODO Check the scan option

				scanDocument((ComboBox<DocumentCategoryDto>) getField(uiSchemaDTO.getId()), uiSchemaDTO.getSubType(),
						false);
			}
		});
		scanButton.hoverProperty().addListener((ov, oldValue, newValue) -> {
			if (newValue) {
				scanButton.setGraphic(new ImageView(new Image(
						this.getClass().getResourceAsStream(RegistrationConstants.SCAN_FOCUSED), 12, 12, true, true)));
			} else {
				scanButton.setGraphic(new ImageView(new Image(
						this.getClass().getResourceAsStream(RegistrationConstants.SCAN), 12, 12, true, true)));
			}
		});
	}

	private <T> ComboBox<DocumentCategoryDto> getComboBox(String id, String titleText, String styleClass,
			double prefWidth, boolean isDisable) {
		ComboBox<DocumentCategoryDto> field = new ComboBox<DocumentCategoryDto>();
		StringConverter<T> uiRenderForComboBox = FXUtils.getInstance().getStringConverterForComboBox();
		VBox vbox = new VBox();
		field.setId(id);
		field.setPrefWidth(prefWidth);
		field.setPromptText(titleText);
		field.setDisable(isDisable);
		field.setConverter((StringConverter<DocumentCategoryDto>) uiRenderForComboBox);
		field.getStyleClass().add(styleClass);
		field.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
			getField(uiSchemaDTO.getId() + RegistrationConstants.LABEL).setVisible(true);
		});

		changeNodeOrientation(field, getRegistrationDTo().getSelectedLanguagesByApplicant().get(0));

		return field;
	}

	private TextField getTextField(String id, String titleText, String demographicTextfield, double prefWidth,
			boolean isDisable) {

		/** Text Field */
		TextField textField = new TextField();
		textField.setId(id);
		textField.setPromptText(titleText);
		textField.getStyleClass().add(RegistrationConstants.DEMOGRAPHIC_TEXTFIELD);
		textField.setPrefWidth(prefWidth);
		textField.setDisable(isDisable);

		return textField;
	}

	@Override
	public void fillData(Object data) {

		ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiSchemaDTO.getId());

		if (data != null) {

			List<DocumentCategoryDto> vals = (List<DocumentCategoryDto>) data;
			comboBox.getItems().addAll(vals);
			new ComboBoxAutoComplete<DocumentCategoryDto>(comboBox);
		}

	}

	public boolean canContinue() {
		if (getRegistrationDTo().getRegistrationCategory().equalsIgnoreCase(RegistrationConstants.PACKET_TYPE_LOST)) {
			return true;
		}

		if (requiredFieldValidator == null) {
			requiredFieldValidator = Initialization.getApplicationContext().getBean(RequiredFieldValidator.class);
		}

		boolean isRequired = requiredFieldValidator.isRequiredField(this.uiSchemaDTO, getRegistrationDTo());
		if (isRequired && getRegistrationDTo().getDocuments().get(this.uiSchemaDTO.getId()) == null) {
			
			Label label = (Label) getField(uiSchemaDTO.getId() + RegistrationConstants.LABEL);
			label.getStyleClass().clear();
			label.getStyleClass().add(RegistrationConstants.DemoGraphicFieldMessageLabel);
			label.setVisible(true);
			return false;
		}

		return true;
	}

	@Override
	public void selectAndSet(Object data) {

		ComboBox<DocumentCategoryDto> comboBox = (ComboBox<DocumentCategoryDto>) getField(uiSchemaDTO.getId());

		if (comboBox != null) {
			comboBox.getSelectionModel().selectFirst();

			DocumentDto documentDto = (DocumentDto) data;

			getRegistrationDTo().addDocument(this.uiSchemaDTO.getId(), documentDto);

			TextField textField = (TextField) getField(uiSchemaDTO.getId() + RegistrationConstants.DOC_TEXT_FIELD);

			textField.setText(documentDto.getRefNumber());

			getField(uiSchemaDTO.getId() + PREVIEW_ICON).setVisible(true);
			getField(uiSchemaDTO.getId() + CLEAR_ID).setVisible(true);
		}
	}
}
