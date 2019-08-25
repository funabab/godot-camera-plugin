![Logo](https://raw.githubusercontent.com/funabab/godot-camera-plugin-demo/master/images/icon_plugin.png)

Godot Camera Plugin (For 3.2 and and above)
=======

A plugin for displaying native camera preview in @godotengine (currently Android only)

## How to use:
Make sure you are running godotengine version 3.2 or above and that your project have been setup for exporting with [custom Android template](https://docs.godotengine.org/en/latest/getting_started/workflow/export/android_custom_build.html).

Download and incude the plugin in your project, then enable it from the Plugin tab in your project settings.

A custom godot control (**CameraView**) is provided which serve as a dummy view for communicating with the *real native camera preview*.

You can also checkout [this demo project](http://github.com/funabab/godot-camera-plugin-demo/) for example of usage.

## Implementation and Quirks
This plugin was design as a proof of concept and also an oppurtunity to try out the new method for building custom Android plugin starting with godotengine 3.2.

It was implemented using custom view drawn over godotengine game view, which means it does not respect godot view hierarchy, also godot related stuff e.g shaders, will not work.

**Note**: Most provided features are still experimental, kindly test and report any bug or issues.

## API
### Properties:
* (bool) **draw_camera_splash**  - *[default true]* whether or not the CameraView control should draw a splash (icon) which will be displayed when the **native camera preview** is not yet drawn.

* (int) **image_resolution** - *[default 10]* A value ranging from 1 - 10 which determines the final size of the of the captured image. **Note** that the final size of the captured image will vary on different devices.

* (int) **camera_facing** - *[default 0]* A value ranging from 0 - 1 which specifies the preview current camera facing. 0 = **Back**, 1 = **Front**.

* (int) **flash_mode** - *[default 1]* A value ranging from 0 - 4 which specifies the preview current camera flash mode. 0 = **Off**, 1 = **Auto**, 2 = **On**, 3 = **Red Eye**, 4 = **Touch**.

* (bool) **pinch_to_zoom** - *[default true]* Whether or not to enable pinch to zoom on the native camera preview.

* (bool) **face_recognition** - *[default true]* Whether or not to enable facial recognition.

* (Color) **face_recognition_bound_color** - *[default Color.white]* - The color of the face bound in the camera preview. NOTE: this will only work if ***face_recognition*** is enabled.

* (int) **face_recognition_bound_shape** - *[default 0]* - A value ranging from 0 - 1 which specify what shape should be used to draw face bounds. 0 = **Rect** 1 = **Circle**. NOTE: this will only work if ***face_recognition*** is enabled.

* (int) **face_recognition_bound_line_size** - *[default 5]* - A value used to specify the line size of the face bound rect. NOTE: this will only work if ***face_recognition*** is enabled.

* (int) **scene_mode** - *[default 0]* - A value ranging from 0 - 5, used to specify the preview current camera image scene mode. 0 = **Auto**, 1 = **HDR**, 2 = **Portrait**, 3 = **Landscape**, 4 = **Night**, 5 = **Sunset**.

* (int) **white_balanace** - *[default 0]* - A value ranging from 0 - 5, used to specify the preview current camera image white balance. 0 = **Auto**, 1 = **Incandesent**, 2 = **Flourescent**, 3 = **Daylight**, 4 = **Twilight**, 5 = **Shade**.

* (int) **color_effect** - *[default 0]* - A value ranging from 0 - 4, used to specify the color errect that will be applied to the preivew current camera image. 0 = **None**, 1 = **Mono**, 2 = **Negative**, 3 = **Solarize**, 4 = **Sepia**.

## Enums:
* **ERROR** - This should be used to check against the *error_code* that will be passed with the ***picture_taken*** signal. Values can be:

    * **NONE** = No error occured.
    * **FATAL_ERROR** = A fatal error occured.
    * **OUT_OF_MEMORY** = An OutOfMemory error occured.
    * **MININUM_NUMBER_OF_FACE_NOT_DETECTED** = The number of face in camera view at capture time is less than the number specified when calling the ***take_picture*** method.

### Methods:
* (bool) **is_color_effect_supported()** - Will return a boolean value specifying if color effects is supported with preview current camera. **Note**: This method will return False if camera is not yet currently intialized (i.e ***is_initialized*** signal not yet emitted)

* (bool) **is_face_recognition_supported** - Will return a boolean value specifying if facial recognition is supported with preview current camera. **Note**: This method will return **False** if camera is not yet currently intialized (i.e ***is_initialized*** signal not yet emitted)

* (bool) **is_flash_mode_supported** - Will return a boolean value specifying if flash mode settings is supported with preview current camera. **Note**: This method will return **False** if camera is not yet currently intialized (i.e ***is_initialized*** signal not yet emitted)

* (bool) **is_pinch_to_zoom_supported** - Will return a boolean value specifying if pinch to zoom is supported with preview current camera. **Note**: This method will return **False** if camera is not yet currently intialized (i.e *is_initialized* signal not yet emitted)

* (bool) **is_scene_mode_supported** - Will return a boolean value specifying if scene mode settings is supported with preview current camera. **Note**: This method will return **False** if camera is not yet currently intialized (i.e ***is_initialized*** signal not yet emitted)

* (bool) **is_whitebalance_mode_supported** - Will return a boolean value specifying if white balance settings is supported with preview current camera. **Note**: This method will return False if camera is not yet currently intialized (i.e ***is_initialized*** signal not yet emitted)

* (void) **take_picture(** *minimum_number_of_face = 0* **)** - Calling this method will initailize the image capture process. The image capture is an asynchronous process, therefore ***picture_taken*** signal will be emitted when the captured image is ready. An optional [*(int)* *minimum_number_of_face*] can also be passed to specify the mininum number of face required for image to be captured. **Note**: Not all devices/cameras have face recognition support. Also most devices have a maximum number of face that can be detected.

### Signals:
* **initialized** - this is emitted whenever the preview current camera changes and the camera have been intialized.

* **picture_taken** [*error_code*: (int), *image_texture*: (ImageTexture), *extras*: Dictionary] - This is emitted whenever ***take_picture([*(int)* *minimum_number_of_face*])*** method is called and image capture process is completed. Note that this signal will be emitted even if the image capture failed. It is advicable to check the return *error_code* against the ***ERROR*** enum. *extras* is a Dictionary containing extra information e.g "faces" if face_recognition is enabled.

## Screenshots
![Screenshot1](https://raw.githubusercontent.com/funabab/godot-camera-plugin-demo/master/images/Screenshot1.png)
![Screenshot2](https://raw.githubusercontent.com/funabab/godot-camera-plugin-demo/master/images/Screenshot2.png)

## SHA u EAT... SHA i EAT?
### Donations if you mind supporting:
**BTC**: 1QHQ9ym6eFkYz7r9Szrw8NSqAQvruV4LT3

**ETH**: 0x7340e216cf8f61f61b5472a12eec50c1ecd40932