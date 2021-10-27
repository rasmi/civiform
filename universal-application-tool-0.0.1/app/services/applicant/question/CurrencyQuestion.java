package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import services.Path;
import services.applicant.Currency;
import services.applicant.ValidationErrorMessage;
import services.question.types.CurrencyQuestionDefinition;
import services.question.types.QuestionType;

/**
 * Represents a currency question in the context of a specific applicant.
 *
 * <p>See {@link ApplicantQuestion} for details.
 */
public class CurrencyQuestion implements PresentsErrors {

  private final ApplicantQuestion applicantQuestion;
  private Optional<Currency> currency;

  public CurrencyQuestion(ApplicantQuestion applicantQuestion) {
    this.applicantQuestion = applicantQuestion;
    assertQuestionType();
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getCurrencyPath());
  }

  @Override
  public boolean hasQuestionErrors() {
    return !getQuestionErrors().isEmpty();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getQuestionErrors() {
    return ImmutableSet.of();
  }

  @Override
  public boolean hasTypeSpecificErrors() {
    return !getAllTypeSpecificErrors().isEmpty();
  }

  @Override
  public ImmutableSet<ValidationErrorMessage> getAllTypeSpecificErrors() {
    // There are no inherent requirements in a currency question.
    return ImmutableSet.of();
  }

  @Override
  public boolean isAnswered() {
    return applicantQuestion.getApplicantData().hasPath(getCurrencyPath());
  }

  public Optional<Currency> getValue() {
    if (currency != null) {
      return currency;
    }

    currency = applicantQuestion.getApplicantData().readCurrency(getCurrencyPath());
    return currency;
  }

  public void assertQuestionType() {
    if (!applicantQuestion.getType().equals(QuestionType.CURRENCY)) {
      throw new RuntimeException(
          String.format(
              "Question is not a CURRENCY question: %s (type: %s)",
              applicantQuestion.getQuestionDefinition().getQuestionPathSegment(),
              applicantQuestion.getQuestionDefinition().getQuestionType()));
    }
  }

  public CurrencyQuestionDefinition getQuestionDefinition() {
    assertQuestionType();
    return (CurrencyQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public Path getCurrencyPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.CURRENCY_CENTS);
  }

  /**
   * Returns the currency value as decimal string with at least 1 dollars digit and always 2 cents
   * digits.
   *
   * <p>Returns "-" if there is no answer.
   */
  @Override
  public String getAnswerString() {
    return getValue().map(value -> value.getDollarsString()).orElse("-");
  }
}