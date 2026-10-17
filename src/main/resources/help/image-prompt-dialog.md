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

Prepare images for the form that opened this dialog. Light and dark images
are separate choices; accepting them returns them to that form, not directly
to persistent storage.

Open a chapter for the task you need. **Expand all** opens every chapter;
**Collapse all** returns to the overview.

## Describe the subject once

![Avatar description](images/help/light/ImagePromptDialog/avatar-prompt-field.png)

The **Avatar Prompt** describes the subject used for both theme variants.
Use **Avatar Negative Prompt** for things to exclude from the avatar.

![Avatar exclusions](images/help/light/ImagePromptDialog/negative-prompt-field.png)

**Generate** starts the light avatar and then its matching dark variant.
For forms supporting headers it also generates the header pair. The avatar
description is required; header-enabled forms also need a header description.

![Generate images](images/help/light/ImagePromptDialog/generate-image-button.png)

Generation needs the image service. If it is unavailable or fails, read the
warning and check the previews; do not assume every variant was replaced.
Accept and image-editing controls are disabled while work is in progress.

## Review both themes before accepting

![Light avatar preview and actions](images/help/light/ImagePromptDialog/light-column.png)

![Dark avatar preview and actions](images/help/light/ImagePromptDialog/dark-column.png)

Each column has **upload**, **download** and **regenerate** actions. Upload a
PNG to replace that variant; uploaded images are resized to 256 by 256 pixels.
Download exports the current image without saving the parent form.

Regenerating the light avatar starts the matching dark generation too, and
also headers when enabled. Regenerating the dark avatar targets the dark
variant using the available light image. Review both after regeneration.

## Prepare headers when the parent supports them

![Header description](images/help/light/ImagePromptDialog/header-prompt-field.png)

Headers are offered for products, features and sprints, not user avatars.
Describe the header's content without theme or style instructions.

![Header style](images/help/light/ImagePromptDialog/image-style-select.png)

![Choose a random style](images/help/light/ImagePromptDialog/random-image-style-button.png)

The style controls header generation. The dice button chooses a concrete
random style; changing the selection does not regenerate existing previews.

![Light header actions](images/help/light/ImagePromptDialog/light-header-column.png)

![Dark header actions](images/help/light/ImagePromptDialog/dark-header-column.png)

The header columns have their own upload/download/regenerate actions.
Regenerating one header does not replace the avatar. Header uploads currently
use the same square resizing as avatar uploads; do not expect the original
wide dimensions to be retained.

## Return the choices to the parent form

![Accept prepared images](images/help/light/ImagePromptDialog/accept-image-button.png)

![Cancel image choices](images/help/light/ImagePromptDialog/cancel-image-button.png)

**Accept** returns the current images and descriptions to the parent form.
You must then **Save** that form to apply them. **Cancel** here discards
unaccepted choices; cancelling the parent discards accepted but unsaved ones.
Downloading an image is independent of either action.
