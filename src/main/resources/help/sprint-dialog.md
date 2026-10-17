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

Create a sprint within a feature or rename an existing sprint. Scheduling and
task planning continue on the sprint's planning pages, not in this form.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Identify the sprint in the right feature

![Sprint name](images/help/light/SprintDialog/sprint-name-field.png)

Enter a required name unique within the parent feature. Creation uses the
feature context supplied by the page that opened the form. Enter through the
intended feature rather than relying on a combined list's visible rows.
The reserved **Backlog** name cannot be used to create another Backlog sprint.

Editing changes the existing sprint without moving it between features or
replacing its tasks. Product access still determines who can work with it.

## Prepare optional images

![Open image generation](images/help/light/SprintDialog/generate-sprint-image-button.png)

The magic wand is shown when Stable Diffusion is available and enabled when
the name is not empty. A custom icon or header is optional.

**Accept** in the image dialog only returns choices to this form. **Save**
here applies them; cancelling this form discards unsaved image choices too.

## Save without changing sprint execution

![Save sprint](images/help/light/SprintDialog/save-sprint-button.png)

![Cancel sprint changes](images/help/light/SprintDialog/cancel-sprint-button.png)

**Save** creates or updates the sprint and closes the form after success.
This is not the action for starting or finishing a sprint, selecting its
team, or changing dependencies.

Resolve a duplicate-name error within the intended feature. Sprint and image
updates use separate requests; after an error, inspect the current sprint
before retrying. Recorded planning changes can be reviewed through the parent
product's history.
