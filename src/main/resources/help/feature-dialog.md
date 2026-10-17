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

Create an area of work within a version, or change an existing feature without
replacing its sprints.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Keep work in the intended version

![Feature name](images/help/light/FeatureDialog/feature-name-field.png)

A feature name is required and must be unique **within its version**. Creation
uses the version context from the page that opened this dialog, not every
version currently visible in a combined list. Enter through the intended
version before creating work.

Editing renames the existing feature; it does not move it to another version
or remove its sprints. Access is inherited from the parent product.

## Choose optional identifying images

![Open image generation](images/help/light/FeatureDialog/generate-feature-image-button.png)

The magic wand is available only when the Stable Diffusion service is
available, and needs a non-empty name. Images are optional.

In the image dialog, **Accept** returns the selected icon/header to this form.
It does not save the feature. **Cancel** here also discards those unsaved
image choices.

## Apply the feature changes

![Save feature](images/help/light/FeatureDialog/save-feature-button.png)

![Cancel feature changes](images/help/light/FeatureDialog/cancel-feature-button.png)

**Save** applies the name and any selected images and closes the dialog after
success. Creating a feature does not automatically create sprints.

For a duplicate name, choose another within the same version. Name and image
updates are separate requests: if an error occurs, reopen the feature and
inspect its current values before retrying. Use the parent product's planning
history to review recorded changes, not to infer that every image change can
be undone.
