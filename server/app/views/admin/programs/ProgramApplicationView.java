package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.option;
import static j2html.TagCreator.p;
import static j2html.TagCreator.select;
import static j2html.TagCreator.span;

import annotations.BindingAnnotations.EnUsLang;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.inject.Inject;
import j2html.tags.DomContent;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.SelectTag;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalLong;
import models.Application;
import org.apache.commons.lang3.RandomStringUtils;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import services.applicant.AnswerData;
import services.applicant.Block;
import services.program.StatusDefinitions;
import views.BaseHtmlLayout;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.components.Modal.Width;
import views.components.ToastMessage;
import views.style.AdminStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Renders a page for a program admin to view a single submitted application. */
public final class ProgramApplicationView extends BaseHtmlView {

  private static final String PROGRAM_ID = "programId";
  private static final String APPLICATION_ID = "applicationId";
  public static final String SEND_EMAIL = "sendEmail";
  public static final String CURRENT_STATUS = "currentStatus";
  public static final String NEW_STATUS = "newStatus";
  public static final String NOTE = "note";
  private final BaseHtmlLayout layout;
  private final Messages enUsMessages;

  @Inject
  public ProgramApplicationView(BaseHtmlLayout layout, @EnUsLang Messages enUsMessages) {
    this.layout = checkNotNull(layout);
    this.enUsMessages = checkNotNull(enUsMessages);
  }

  public Content render(
      long programId,
      String programName,
      Application application,
      String applicantNameWithApplicationId,
      ImmutableList<Block> blocks,
      ImmutableList<AnswerData> answers,
      StatusDefinitions statusDefinitions,
      Optional<String> noteMaybe,
      Http.Request request) {
    String title = "Program Application View";
    ListMultimap<Block, AnswerData> blockToAnswers = ArrayListMultimap.create();
    for (AnswerData answer : answers) {
      Block answerBlock =
          blocks.stream()
              .filter(block -> block.getId().equals(answer.blockId()))
              .findFirst()
              .orElseThrow();
      blockToAnswers.put(answerBlock, answer);
    }

    ImmutableList<Modal> statusUpdateConfirmationModals =
        statusDefinitions.getStatuses().stream()
            .map(
                status ->
                    renderStatusUpdateConfirmationModal(
                        programId,
                        programName,
                        application,
                        applicantNameWithApplicationId,
                        status))
            .collect(ImmutableList.toImmutableList());
    Modal updateNoteModal = renderUpdateNoteConfirmationModal(programId, application, noteMaybe);

    DivTag contentDiv =
        div()
            .withId("application-view")
            .withClasses(Styles.PX_20)
            .with(
                h2("Program: " + programName).withClasses(Styles.MY_4),
                div()
                    .withClasses(Styles.FLEX)
                    .with(
                        p(applicantNameWithApplicationId)
                            .withClasses(
                                Styles.MY_4,
                                Styles.TEXT_BLACK,
                                Styles.TEXT_2XL,
                                Styles.MB_2,
                                ReferenceClasses.BT_APPLICATION_ID),
                        // Spread out the items, so the following are right
                        // aligned.
                        p().withClasses(Styles.FLEX_GROW))
                    // Status options if configured on the program.
                    .condWith(
                        !statusDefinitions.getStatuses().isEmpty(),
                        div()
                            .withClasses(Styles.FLEX, Styles.MR_4, Styles.SPACE_X_2)
                            .with(
                                renderStatusOptionsSelector(application, statusDefinitions),
                                updateNoteModal.getButton()))
                    .with(renderDownloadButton(programId, application.id)))
            .with(
                each(
                    blocks,
                    block -> renderApplicationBlock(programId, block, blockToAnswers.get(block))))
            .with(each(statusUpdateConfirmationModals, Modal::getButton));

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainContent(contentDiv)
            // The body and main styles are necessary for modals to appear since they use fixed
            // sizing.
            .addBodyStyles(Styles.FLEX)
            .addMainStyles(Styles.W_SCREEN)
            .addModals(updateNoteModal)
            .addModals(statusUpdateConfirmationModals)
            .addFooterScripts(layout.viewUtils.makeLocalJsTag("admin_application_view"));
    Optional<String> maybeSuccessMessage = request.flash().get("success");
    if (maybeSuccessMessage.isPresent()) {
      htmlBundle.addToastMessages(ToastMessage.success(maybeSuccessMessage.get()));
    }
    return layout.render(htmlBundle);
  }

  private ATag renderDownloadButton(long programId, long applicationId) {
    String link =
        controllers.admin.routes.AdminApplicationController.download(programId, applicationId)
            .url();
    return new LinkElement().setHref(link).setText("Export to PDF").asRightAlignedButton();
  }

  private DivTag renderApplicationBlock(
      long programId, Block block, Collection<AnswerData> answers) {
    DivTag topContent =
        div()
            .withClasses(Styles.FLEX)
            .with(
                div(
                    div(block.getName())
                        .withClasses(
                            Styles.TEXT_BLACK, Styles.FONT_BOLD, Styles.TEXT_XL, Styles.MB_2)))
            .with(p().withClasses(Styles.FLEX_GROW))
            .with(p(block.getDescription()).withClasses(Styles.TEXT_GRAY_700, Styles.ITALIC));

    DivTag mainContent =
        div()
            .withClasses(Styles.W_FULL)
            .with(each(answers, answer -> renderAnswer(programId, answer)));

    DivTag innerDiv =
        div(topContent, mainContent)
            .withClasses(
                Styles.BORDER, Styles.BORDER_GRAY_300, Styles.BG_WHITE, Styles.ROUNDED, Styles.P_4);

    return div(innerDiv)
        .withClasses(
            ReferenceClasses.ADMIN_APPLICATION_BLOCK_CARD,
            Styles.W_FULL,
            Styles.SHADOW_LG,
            Styles.MB_4);
  }

  private DivTag renderAnswer(long programId, AnswerData answerData) {
    LocalDate date =
        Instant.ofEpochMilli(answerData.timestamp()).atZone(ZoneId.systemDefault()).toLocalDate();
    DivTag answerContent;
    if (answerData.encodedFileKey().isPresent()) {
      String encodedFileKey = answerData.encodedFileKey().get();
      String fileLink =
          controllers.routes.FileController.adminShow(programId, encodedFileKey).url();
      answerContent = div(a(answerData.answerText()).withHref(fileLink));
    } else {
      answerContent = div(answerData.answerText());
    }
    return div()
        .withClasses(Styles.FLEX)
        .with(
            div()
                .withClasses(Styles.MB_8)
                .with(
                    div(answerData.questionDefinition().getName())
                        .withClasses(Styles.TEXT_GRAY_400, Styles.TEXT_BASE, Styles.LINE_CLAMP_3)))
        .with(p().withClasses(Styles.W_8))
        .with(
            answerContent.withClasses(Styles.TEXT_GRAY_700, Styles.TEXT_BASE, Styles.LINE_CLAMP_3))
        .with(p().withClasses(Styles.FLEX_GROW))
        .with(
            div("Answered on " + date)
                .withClasses(
                    Styles.FLEX_AUTO, Styles.TEXT_RIGHT, Styles.FONT_LIGHT, Styles.TEXT_XS));
  }

  private DivTag renderStatusOptionsSelector(
      Application application, StatusDefinitions statusDefinitions) {
    final String SELECTOR_ID = RandomStringUtils.randomAlphabetic(8);
    DivTag container =
        div()
            .withClasses(Styles.FLEX, ReferenceClasses.PROGRAM_ADMIN_STATUS_SELECTOR)
            .with(label("Status:").withClasses(Styles.SELF_CENTER).withFor(SELECTOR_ID));

    SelectTag dropdownTag =
        select()
            .withId(SELECTOR_ID)
            .withClasses(
                Styles.OUTLINE_NONE,
                Styles.PX_3,
                Styles.PY_1,
                Styles.ML_3,
                Styles.MY_4,
                Styles.BORDER,
                Styles.BORDER_GRAY_500,
                Styles.ROUNDED_FULL,
                Styles.BG_WHITE,
                Styles.TEXT_XS,
                StyleUtils.focus(BaseStyles.BORDER_SEATTLE_BLUE));

    // Add the options available to the admin.
    // When no status is currently applied to the application, add a placeholder option that is
    // selected.
    dropdownTag.with(
        option(enUsMessages.at(MessageKey.DROPDOWN_PLACEHOLDER.getKeyName()))
            .isDisabled()
            .withCondSelected(!application.getLatestStatus().isPresent()));

    // Add statuses in the order they're provided.
    String latestStatusText = application.getLatestStatus().orElse("");
    statusDefinitions.getStatuses().stream()
        .map(StatusDefinitions.Status::statusText)
        .forEach(
            statusText -> {
              boolean isCurrentStatus = statusText.equals(latestStatusText);
              dropdownTag.with(
                  option(statusText).withValue(statusText).withCondSelected(isCurrentStatus));
            });
    return container.with(dropdownTag);
  }

  private Modal renderUpdateNoteConfirmationModal(
      long programId, Application application, Optional<String> noteMaybe) {
    ButtonTag triggerButton =
        makeSvgTextButton("Edit note", Icons.EDIT).withClasses(AdminStyles.TERTIARY_BUTTON_STYLES);
    String formId = Modal.randomModalId();
    // No form action or content is rendered since admin_application_view.ts extracts the values
    // and calls postMessage rather than attempting a submission. The main frame is responsible for
    // constructing a form to update the note.
    FormTag modalContent =
        form()
            .withId(formId)
            .withClasses(Styles.PX_6, Styles.PY_2, "cf-program-admin-edit-note-form");
    modalContent.with(
        input().withName(PROGRAM_ID).withValue(Long.toString(programId)).isHidden(),
        input().withName(APPLICATION_ID).withValue(Long.toString(application.id)).isHidden(),
        FieldWithLabel.textArea()
            .setValue(noteMaybe)
            .setFormId(formId)
            .setFieldName(NOTE)
            .setRows(OptionalLong.of(8))
            .getTextareaTag(),
        div()
            .withClasses(Styles.FLEX, Styles.MT_5, Styles.SPACE_X_2)
            .with(
                div().withClass(Styles.FLEX_GROW),
                button("Cancel")
                    .withClasses(ReferenceClasses.MODAL_CLOSE, AdminStyles.TERTIARY_BUTTON_STYLES),
                submitButton("Save").withClass(AdminStyles.TERTIARY_BUTTON_STYLES)));
    return Modal.builder(Modal.randomModalId(), modalContent)
        .setModalTitle("Edit note")
        .setTriggerButtonContent(triggerButton)
        .setWidth(Width.THREE_FOURTHS)
        .build();
  }

  private Modal renderStatusUpdateConfirmationModal(
      long programId,
      String programName,
      Application application,
      String applicantNameWithApplicationId,
      StatusDefinitions.Status status) {
    // The previous status as it should be displayed and passed as data in the
    // update.
    String previousStatusDisplay = application.getLatestStatus().orElse("Unset");
    String previousStatusData = application.getLatestStatus().orElse("");
    // No form action or content is rendered since admin_application_view.ts extracts the values
    // and calls postMessage rather than attempting a submission. The main frame is responsible for
    // constructing a form to update the status.
    FormTag modalContent =
        form()
            .withClasses(Styles.PX_6, Styles.PY_2, "cf-program-admin-status-update-form")
            .with(
                input().withName(PROGRAM_ID).withValue(Long.toString(programId)).isHidden(),
                input()
                    .withName(APPLICATION_ID)
                    .withValue(Long.toString(application.id))
                    .isHidden(),
                p().with(
                        span("Status Change: "),
                        span(previousStatusDisplay).withClass(Styles.FONT_SEMIBOLD),
                        span(" -> ").withClass(Styles.FONT_SEMIBOLD),
                        span(status.statusText()).withClass(Styles.FONT_SEMIBOLD),
                        span(" (visible to applicant)")),
                p().with(
                        span("Applicant: "),
                        span(applicantNameWithApplicationId)
                            .withClasses(Styles.FONT_SEMIBOLD, ReferenceClasses.BT_APPLICATION_ID)),
                p().with(span("Program: "), span(programName).withClass(Styles.FONT_SEMIBOLD)),
                div()
                    .withClasses(Styles.MT_4)
                    // Add the new status to the form hidden.
                    .with(
                        input()
                            .isHidden()
                            .withType("text")
                            .withName(NEW_STATUS)
                            .withValue(status.statusText()))
                    // Add the original status to the form hidden so that we can
                    // detect if the data has changed since this UI was
                    // generated.
                    .with(
                        input()
                            .isHidden()
                            .withType("text")
                            .withName(CURRENT_STATUS)
                            .withValue(previousStatusData))
                    .with(
                        renderStatusUpdateConfirmationModalEmailSection(
                            applicantNameWithApplicationId, application, status)),
                div()
                    .withClasses(Styles.FLEX, Styles.MT_5, Styles.SPACE_X_2)
                    .with(
                        div().withClass(Styles.FLEX_GROW),
                        button("Cancel")
                            .withClasses(
                                ReferenceClasses.MODAL_CLOSE, AdminStyles.TERTIARY_BUTTON_STYLES),
                        submitButton("Confirm").withClass(AdminStyles.TERTIARY_BUTTON_STYLES)));
    ButtonTag triggerButton =
        button("")
            .withClasses(Styles.HIDDEN)
            .withData("status-update-confirm-for-status", status.statusText());
    return Modal.builder(Modal.randomModalId(), modalContent)
        .setModalTitle("Change the status of this application?")
        .setWidth(Width.THREE_FOURTHS)
        .setTriggerButtonContent(triggerButton)
        .build();
  }

  private DomContent renderStatusUpdateConfirmationModalEmailSection(
      String applicantNameWithApplicationId,
      Application application,
      StatusDefinitions.Status status) {
    InputTag sendEmailInput =
        input().withType("checkbox").withName(SEND_EMAIL).withClasses(BaseStyles.CHECKBOX);
    Optional<String> maybeApplicantEmail =
        Optional.ofNullable(application.getApplicant().getAccount().getEmailAddress());
    if (!status.localizedEmailBodyText().isPresent()) {
      return div()
          .with(
              sendEmailInput.isHidden(),
              p().with(
                      span(applicantNameWithApplicationId)
                          .withClasses(Styles.FONT_SEMIBOLD, ReferenceClasses.BT_APPLICATION_ID),
                      span(
                          " will not receive an email because there is no email content set for"
                              + " this status. Connect with your CiviForm Admin to add an email to"
                              + " this status.")));
    } else if (maybeApplicantEmail.isEmpty()) {
      return div()
          .with(
              sendEmailInput.isHidden(),
              p().with(
                      span(applicantNameWithApplicationId).withClass(Styles.FONT_SEMIBOLD),
                      span(
                          " will not receive an email for this change since they have not provided"
                              + " an email address.")));
    }
    return label()
        .with(
            // Check by default when visible.
            sendEmailInput.isChecked(),
            span("Notify "),
            span(applicantNameWithApplicationId).withClass(Styles.FONT_SEMIBOLD),
            span(" of this change at "),
            span(maybeApplicantEmail.orElse("")).withClass(Styles.FONT_SEMIBOLD));
  }
}
