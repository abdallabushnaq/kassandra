<!--
Copyright (C) 2025-2026 Abdalla Bushnaq
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

## Create or edit a product

This dialog lets you set the product name, create an icon/header, and choose who
can access the product. **Create Product** adds a new product; **Edit Product**
changes an existing one.

![Create Product fields and buttons with numbered references](images/help/light/product-create.png)

| Reference | Field or button | How to use it |
|-----------|-----------------|---------------|
| 1 | **Product Name** | Enter a required, unique name, for example **Jupiter**. |
| 2 | **Grant Access to Users** | Select individual collaborators. Multiple entries are allowed. |
| 3 | **Grant Access to Groups** | Select groups of collaborators. Multiple entries are allowed. |
| 4 | **Save** | Save the product and close the dialog once the form is valid. |
| 5 | **Cancel** | Close without saving your form changes. |

The screenshot shows the Create dialog without image generation available. The
Edit dialog uses the same fields, populated with the existing product settings.

## Who has access by default?

When you create a product and leave both access selectors empty, **you, the
creator, receive access automatically**. **Administrators can access all
products**. No other users or groups are granted access automatically.

To collaborate, select people in **Grant Access to Users** or groups in
**Grant Access to Groups**. For example, selecting **Team** gives that group's
members access. These grants cover the product and its versions, features and
sprints.

When editing, the selectors show existing explicit grants, including the
creator's grant. Removing a selection removes that grant when you save; access
through another selected group or an administrator role is independent.
Check access changes carefully to avoid removing your own access unintentionally.

## Generate a new icon or header

When AI image generation is configured and available, a **magic-wand button
beside Product Name** opens the image-generation dialog. Enter a name first to
enable it. The button is not visible in the example screenshot.

The image-generation dialog lets you describe and generate product images,
including icons and headers, or upload an existing image. Select **Accept**
to return the chosen images to the product form, then **Save** to apply them
to the product. Accepting an image alone does not save the product.

## Save or cancel

Select **Save** after completing the required name and any optional settings.
If the name is missing or already used, correct the displayed error and try again.
Select **Cancel** to discard unsaved form changes.

Saved product changes are recorded in product-scoped history. Return to the
Products page and use the **history button in the top navigation** to undo or
redo recorded changes for that product.

## Close help

Select the **X** at the top of this drawer, or press **Escape** while focus is
inside it. You return to the product form; closing help does not save or cancel
the form.
