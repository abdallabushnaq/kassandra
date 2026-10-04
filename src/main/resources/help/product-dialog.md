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

## Set up a product or change an existing one

Use **Create Product** to start a separate body of work. Use **Edit Product**
to rename an existing product or change its collaborators and optional images
without replacing its versions, features or sprints.

The product name is required and must be unique. Choose a name that collaborators
can distinguish from other products. If you are planning another release of
the same product, add a **version** from its Versions page instead of creating
another product.

Creating a product does not automatically add versions or planned work.
Renaming an existing product keeps its identity and contained work.

## Decide who should be able to work on it

When you create a product and leave both access selectors empty, **you, the
creator, receive access automatically**. **Administrators can access all
products**. No other users or groups are granted access automatically.

To collaborate, select people in **Grant Access to Users** or groups in
**Grant Access to Groups**. Use individual grants for named collaborators and a
group grant when access should follow membership of a team. Both selectors
allow multiple grants. A person needs only one applicable user or group grant;
they do not need to be selected in both.

These grants cover the product and its versions, features and sprints. Changing
them changes who can access the existing work, not just who can see the product
row. Granting a group gives its members access; later membership changes can
therefore affect access without another product edit.

When editing, the selectors show existing explicit grants, including the
creator's grant. Removing a selection removes that grant when you save; access
through another selected group or an administrator role is independent.
To remove someone's access, check all the routes through which they receive it.
**Removing your own last grant can make the product inaccessible to you after
saving.** An administrator's role-based access is not controlled by these
selectors.

## Choose optional identifying images

When AI image generation is configured and available, a **magic-wand button
beside Product Name** opens the image-generation dialog. Enter a name first to
enable it. If the button is absent, the Stable Diffusion service was unavailable
when this dialog was opened; creating or editing a product does not require
custom images.

The image-generation dialog lets you describe and generate product images,
including icons and headers, or upload an existing image. Select **Accept**
to return the chosen images to the product form, then **Save** to apply them
to the product. **Accept is not Save**: cancelling the product form discards
those unsaved choices along with your other form changes.

## Apply the changes or leave the product unchanged

**Save** applies the name, optional images and access-grant changes. After a
successful save, the dialog closes and the list refreshes; a product can
disappear from your current results if its new name no longer matches a filter
or your access has been removed.

If the name is missing or already used, correct the field error. A dialog-level
error can indicate that the operation was not permitted or could not complete.
Do not assume an unsuccessful save left everything untouched: the name, images
and grants are saved in separate steps. If an error occurs after some steps
succeeded, reopen the product and inspect its current settings before retrying.

**Cancel** discards changes that have not been saved; it does not undo an earlier
successful save.

## Recover recorded planning changes

Return to **Products** to find this product's history.

{{include:undo-redo}}
