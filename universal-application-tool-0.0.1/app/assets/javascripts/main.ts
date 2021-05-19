/**
 * We're trying to keep the JS pretty mimimal for CiviForm, so we're only using it
 * where it's necessary to improve the user experience.
 *
 * Appropriate uses include:
 *  - Visual transitions that aren't possible with just CSS.
 *  - Rare instances in which we need to update a page without refreshing.
 *  - TBD
 */

function attachDropdown(elementId: string) {
  const dropdownId = elementId + "-dropdown";
  const element = document.getElementById(elementId);
  const dropdown = document.getElementById(dropdownId);
  if (dropdown && element) {
    // Attach onclick event to element to toggle dropdown visibility.
    element.addEventListener("click", () => toggleElementVisibility(dropdownId));

    // Attach onblur event to page to hide dropdown if it wasn't the clicked element.
    document.addEventListener("click", (e) => maybeHideElement(e, dropdownId, elementId));
  }
}

function toggleElementVisibility(id: string) {
  const element = document.getElementById(id);
  if (element) {
    element.classList.toggle("hidden");
  }
}

function maybeHideElement(e: Event, id: string, parentId: string) {
  if (e.target instanceof Element) {
    const parent = document.getElementById(parentId);
    if (parent && !parent.contains(e.target)) {
      const elementToHide = document.getElementById(id);
      if (elementToHide) {
        elementToHide.classList.add("hidden");
      }
    }
  }
}

/** In admin program block edit form - enabling submit button when form is changed or if not empty */
function changeUpdateBlockButtonState(event: Event) {
  const blockEditForm = document.getElementById("block-edit-form");
  const submitButton = document.getElementById("update-block-button");

  const formNameInput = blockEditForm["block-name-input"];
  const formDescriptionText = blockEditForm["block-description-textarea"];

  if ((formNameInput.value !== formNameInput.defaultValue ||
    formDescriptionText.value !== formDescriptionText.defaultValue) &&
    (formNameInput.value !== "" && formDescriptionText.value !== "")) {
    submitButton.removeAttribute("disabled");
  } else {
    submitButton.setAttribute("disabled", "");
  }
}

/**
 * Copy the specified hidden template and append it to the end of the parent divContainerId,
 * above the add button (addButtonId).
 */
function addNewInput(inputTemplateId: string, addButtonId: string, divContainerId: string) {
  // Copy the answer template and remove ID and hidden properties.
  const newField = document.getElementById(inputTemplateId).cloneNode(true) as HTMLElement;
  newField.classList.remove("hidden");
  newField.removeAttribute("id");

  // Register the click event handler for the remove button.
  newField.querySelector("[type=button]").addEventListener("click", removeInput);

  // Find the add option button and insert the new option input field before it.
  const button = document.getElementById(addButtonId);
  document.getElementById(divContainerId).insertBefore(newField, button);
}

/**
 * Removes an input field and its associated elements, like the remove button. All
 * elements must be contained in a parent div.
 */
function removeInput(event: Event) {
  // Get the parent div, which contains the input field and remove button, and remove it.
  const optionDiv = (event.target as Element).parentNode;
  optionDiv.parentNode.removeChild(optionDiv);

}

/**
 * If we want to remove an existing element, hide the input div and set disabled to false
 * so the field is submitted.
 */
function hideInput(event: Event) {
  const inputDiv = (event.target as Element).parentElement;
  // Remove 'disabled' so the field is submitted with the form
  inputDiv.querySelector("input").disabled = false;
  // Hide the entire div from the user
  inputDiv.classList.add("hidden");
}

/** In the enumerator form - add a new input field for a repeated entity. */
function addNewEnumeratorField(event: Event) {
  // Copy the enumerator field template
  const newField = document.getElementById("enumerator-field-template").cloneNode(true) as HTMLElement;
  newField.classList.remove("hidden");
  newField.removeAttribute("id");

  // Add the remove enumerator field event listener to the delete button
  newField.querySelector("[type=button]").addEventListener("click", removeEnumeratorField);

  // Add to the end of enumerator-fields div.
  const enumeratorFields = document.getElementById("enumerator-fields");
  enumeratorFields.appendChild(newField);
}

function removeEnumeratorField(event: Event) {
  // Get the parent div, which contains the input field and remove button, and remove it.
  const enumeratorFieldDiv = (event.currentTarget as HTMLElement).parentNode;
  enumeratorFieldDiv.parentNode.removeChild(enumeratorFieldDiv);
}

/**
 * Enumerator delete buttons for existing entities behave differently than removing fields that
 * were just added client-side and were not saved server-side.
 */
function removeExistingEnumeratorField(event: Event) {
  // Get the button that was clicked
  const removeButton = event.currentTarget as HTMLElement;

  // Hide the field that was removed. We cannot remove it completely, as we need to
  // submit the input to maintain entity ordering.
  const enumeratorFieldDiv = removeButton.parentElement;
  enumeratorFieldDiv.classList.add("hidden");

  // Create a copy of the hidden deleted entity template. Set the value to this
  // button's ID, and set disabled to false so the data is submitted with the form.
  const deletedEntityInput = document.getElementById("enumerator-delete-template").cloneNode(true) as HTMLInputElement;
  deletedEntityInput.disabled = false;
  deletedEntityInput.setAttribute("value", removeButton.id);
  deletedEntityInput.removeAttribute("id");

  // Add the hidden deleted entity input to the page.
  enumeratorFieldDiv.appendChild(deletedEntityInput);
}

/**
 * Remove line-clamp from div on click.
 * 
 * NOTE: This is in no way discoverable, but it's just a temporary fix until we have a program
 * landing page.
 */
function removeLineClamp(event: Event) {
  const target = event.target as HTMLElement;
  target.classList.add("line-clamp-none");
}

function attachLineClampListeners() {
  const applicationCardDescriptions = Array.from(document.querySelectorAll('.cf-application-card-description'));
  applicationCardDescriptions.forEach(el => el.addEventListener("click", removeLineClamp));
}

window.addEventListener('load', (event) => {
  attachDropdown("create-question-button");

  attachLineClampListeners();

  // Submit button is disabled by default until program block edit form is changed
  const blockEditForm = document.getElementById("block-edit-form");
  if (blockEditForm) {
    blockEditForm.addEventListener("input", changeUpdateBlockButtonState);
  }

  // Configure the button on the admin question form to add more answer options
  const questionOptionButton = document.getElementById("add-new-option");
  if (questionOptionButton) {
    questionOptionButton.addEventListener("click", function() {
      addNewInput("multi-option-question-answer-template", "add-new-option", "question-settings");
    });
  }

  // Bind click handler for remove options in multi-option edit view
  Array.from(document.querySelectorAll('.multi-option-question-field-remove-button')).forEach(
    el => el.addEventListener("click", removeInput));

  // Configure the button on the manage program admins form to add more email inputs
  const adminEmailButton = document.getElementById("add-program-admin-button");
  if (adminEmailButton) {
    adminEmailButton.addEventListener("click", function() {
      addNewInput("program-admin-email-template", "add-program-admin-button", "program-admin-emails");
    });
  }

  // Bind click handler for removing program admins in the program admin management view
  Array.from(document.querySelectorAll('.cf-program-admin-remove-button')).forEach(
    el => el.addEventListener("click", hideInput));

  // Configure the button on the enumerator question form to add more enumerator field options
  const enumeratorOptionButton = document.getElementById("enumerator-field-add-button");
  if (enumeratorOptionButton) {
    enumeratorOptionButton.addEventListener("click", addNewEnumeratorField);
  }

  // Configure existing enumerator entity remove buttons
  Array.from(document.querySelectorAll('.cf-enumerator-delete-button')).forEach(
    el => el.addEventListener("click", removeExistingEnumeratorField));
});
