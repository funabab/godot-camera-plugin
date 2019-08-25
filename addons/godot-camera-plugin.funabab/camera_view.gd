tool
extends Control

const CameraViewNativeBridge = preload("camera_view_native_bridge.gd").CameraViewNativeBridge;
const ERROR_FATAL = 1;
const ICON_CAMERA = preload("icon_camera.png");
const COLOR_BG = Color.black;

enum ERROR {
	NONE = 0,
	FATAL_ERROR = 1,
	OUT_OF_MEMORY = 2,
	MININUM_NUMBER_OF_FACE_NOT_DETECTED = 3
}

#export(int, 1, 100) var image_quality := 100 setget set_image_quality;
export var draw_camera_splash = true setget set_draw_camera_splash;
export(int, 1, 10) var image_resolution := 10 setget set_image_resolution;
export(int, "Back", "Front") var camera_facing := 0 setget set_camera_facing;
export(int, "Off", "Auto", "On", "Red Eye", "Torch") var flash_mode := 1 setget set_flash_mode;
#export var enable_shutter_sound = false setget set_enable_shutter_sound;
export var pinch_to_zoom := true setget set_pinch_to_zoom;
export var face_recognition := true setget set_face_recognition;
export var face_recognition_bound_color := Color.white setget set_face_recognition_bound_color;
export(int, "Rect", "Circle") var face_recognition_bound_shape := 0 setget set_face_recognition_bound_shape;
export(int, 1, 100) var face_recognition_bound_line_size := 5 setget set_face_recognition_bound_line_size;
export(int, "Auto", "HDR", "Portrait", "Landscape", "Night", "Sunset") var scene_mode := 0 setget set_scene_mode;
export(int, "Auto", "Incandesent", "Flourescent", "Daylight", "Twilight", "Shade") var white_balanace := 0 setget set_white_balanace;
export(int, "None", "Mono", "Negative", "Solarize", "Sepia") var color_effect := 0 setget set_color_effect;

var cameraViewBridge setget __set_blank__, __get_blank__;
var is_initialized : bool = false setget __set_blank__;
var camera_permission_granted = false setget __set_blank__, __get_blank__;

signal picture_taken; # @parameters: error_code, image_texture, extra
signal initialized;

func is_face_recognition_supported():
	if cameraViewBridge == null: return false;
	return cameraViewBridge.has_feature(CameraViewNativeBridge.FEATURE_FACE_RECOGNITION);
	pass

func is_flash_mode_supported():
	if cameraViewBridge == null: return false;
	return cameraViewBridge.has_feature(CameraViewNativeBridge.FEATURE_FLASH_MODE);
	pass

func is_pinch_to_zoom_supported():
	if cameraViewBridge == null: return false;
	return cameraViewBridge.has_feature(CameraViewNativeBridge.FEATURE_CAMERA_ZOOM);
	pass

func is_color_effect_supported():
	if cameraViewBridge == null: return false;
	return cameraViewBridge.has_feature(CameraViewNativeBridge.FEATURE_COLOR_EFFECT);
	pass

func is_scene_mode_supported():
	if cameraViewBridge == null: return false;
	return cameraViewBridge.has_feature(CameraViewNativeBridge.FEATURE_SCENE_MODE);
	pass

func is_whitebalance_mode_supported():
	if cameraViewBridge == null: return false;
	return cameraViewBridge.has_feature(CameraViewNativeBridge.FEATURE_WHITE_BALANCE);
	pass

func take_picture(minimum_number_of_face = 0):
	if cameraViewBridge == null: return;
	cameraViewBridge.take_picture(minimum_number_of_face);
	pass

func _draw():
	if !draw_camera_splash: return;

	var rect = get_rect();
	var icon_size = rect.size.x * .3;
	var size = Vector2(icon_size / ICON_CAMERA.get_width(),
		icon_size / ICON_CAMERA.get_width());
	
	draw_rect(rect, COLOR_BG);
	draw_set_transform((rect.size - (size * ICON_CAMERA.get_width())) / 2, 0, size);
	draw_texture(ICON_CAMERA, Vector2(0, 0),
		Color.red if is_initialized else Color.white);
	pass

func _notification(what):
	if Engine.editor_hint:
		if what == NOTIFICATION_RESIZED:
			update();
		return;

	if what == NOTIFICATION_ENTER_TREE:
		if OS.get_name() == "Android":
			camera_permission_granted = OS.request_permission("CAMERA");
			if camera_permission_granted == false:
				# Camera will proberly not start due to permission not granted
	
				# give users some time to grant permission
				yield(get_tree().create_timer(2), "timeout");
				prints("Godot: restart?");
				get_tree().reload_current_scene(); # lets reload and try again
	elif what == NOTIFICATION_RESIZED:
		if !is_initialized && camera_permission_granted:
#			yield(get_tree(), "idle_frame");
			var CameraViewNativeBridge = preload("camera_view_native_bridge.gd");
			cameraViewBridge = CameraViewNativeBridge.initialize(self);
			is_initialized = true;
		else:
			if cameraViewBridge != null:
				cameraViewBridge.resize_view(get_global_rect());

		update();
	elif what == NOTIFICATION_VISIBILITY_CHANGED:
		if cameraViewBridge != null:
			cameraViewBridge.set_view_visibilty(visible);
	elif what == NOTIFICATION_EXIT_TREE:
		if cameraViewBridge != null:
			cameraViewBridge.destroy_view();
	pass

func __get_parameters__()->Dictionary:
	return {
#		CameraViewNativeBridge.PARAMETER_IMAGE_QUALITY: image_quality,
		CameraViewNativeBridge.PARAMETER_IMAGE_RESOLUTION: image_resolution,
		CameraViewNativeBridge.PARAMETER_FLASH_MODE: flash_mode,
#		CameraViewNativeBridge.PARAMETER_ENABLE_SHUTTER_SOUND: enable_shutter_sound,
		CameraViewNativeBridge.PARAMETER_PITCH_TO_ZOOM: pinch_to_zoom,
		CameraViewNativeBridge.PARAMETER_FACE_RECOGNITION: face_recognition,
		CameraViewNativeBridge.PARAMETER_FACE_RECOGNITION_BOUND_COLOR: face_recognition_bound_color.to_html(),
		CameraViewNativeBridge.PARAMETER_FACE_RECOGNITION_BOUND_SHAPE: face_recognition_bound_shape,
		CameraViewNativeBridge.PARAMETER_FACE_RECOGNITION_BOUND_LINE_SIZE: face_recognition_bound_line_size,
		CameraViewNativeBridge.PARAMETER_SCENE_MODE: scene_mode,
		CameraViewNativeBridge.PARAMETER_WHITE_BALANCE: white_balanace,
		CameraViewNativeBridge.PARAMETER_COLOR_EFFECT: color_effect
	}
	pass

func __set_blank__(value):
	# do nothing...
	pass

func __get_blank__():
		# do nothing...
	pass

func set_camera_facing(value):
	camera_facing = value;
	if cameraViewBridge != null:
		cameraViewBridge.set_camera_facing(true if value == 0 else false);
	pass

func get_camera_facing()->bool:
	return camera_facing == 0; # returns bool, TRUE if camera facing is currently set to back
	pass

#func set_image_quality(value):
#	image_quality = value;
#	if cameraViewBridge != null:
#		cameraViewBridge.set_parameter(CameraViewNativeBridge.PARAMETER_IMAGE_QUALITY, value);
#	pass

func set_draw_camera_splash(value):
	draw_camera_splash = value;
	update();
	pass

func set_image_resolution(value):
	image_resolution = value;
	if cameraViewBridge != null:
		cameraViewBridge.set_parameter(CameraViewNativeBridge.PARAMETER_IMAGE_RESOLUTION, value);
	pass

func set_flash_mode(value):
	flash_mode = value;
	if cameraViewBridge != null:
		cameraViewBridge.set_parameter(CameraViewNativeBridge.PARAMETER_FLASH_MODE, flash_mode);
	pass

#func set_enable_shutter_sound(value):
#	enable_shutter_sound = value;
#	if cameraViewBridge != null:
#		cameraViewBridge.set_parameter(CameraViewNativeBridge.PARAMETER_ENABLE_SHUTTER_SOUND, enable_shutter_sound);
#	pass

func set_pinch_to_zoom(value):
	pinch_to_zoom = value;
	if cameraViewBridge != null:
		cameraViewBridge.set_parameter(CameraViewNativeBridge.PARAMETER_PITCH_TO_ZOOM, value);
	pass

func set_face_recognition(value):
	face_recognition = value;
	if cameraViewBridge != null:
		cameraViewBridge.set_parameter(CameraViewNativeBridge.PARAMETER_FACE_RECOGNITION, value);
	pass

func set_face_recognition_bound_color(value):
	face_recognition_bound_color = value;
	if cameraViewBridge != null:
		cameraViewBridge.set_parameter(CameraViewNativeBridge.PARAMETER_FACE_RECOGNITION_BOUND_COLOR, value.to_html());
	pass

func set_face_recognition_bound_shape(value):
	face_recognition_bound_shape = value;
	if cameraViewBridge != null:
		cameraViewBridge.set_parameter(CameraViewNativeBridge.PARAMETER_FACE_RECOGNITION_BOUND_SHAPE, value);
	pass

func set_face_recognition_bound_line_size(value):
	face_recognition_bound_line_size = value;
	if cameraViewBridge != null:
		cameraViewBridge.set_parameter(CameraViewNativeBridge.PARAMETER_FACE_RECOGNITION_BOUND_LINE_SIZE, value);
	pass

func set_scene_mode(value):
	scene_mode = value;
	if cameraViewBridge != null:
		cameraViewBridge.set_parameter(CameraViewNativeBridge.PARAMETER_SCENE_MODE, value);
	pass

func set_white_balanace(value):
	white_balanace = value;
	if cameraViewBridge != null:
		cameraViewBridge.set_parameter(CameraViewNativeBridge.PARAMETER_WHITE_BALANCE, value);
	pass

func set_color_effect(value):
	color_effect = value;
	if cameraViewBridge != null:
		cameraViewBridge.set_parameter(CameraViewNativeBridge.PARAMETER_COLOR_EFFECT, value);
	pass
