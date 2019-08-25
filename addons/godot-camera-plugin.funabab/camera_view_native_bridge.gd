class CameraViewNativeBridge extends Reference:

	const FEATURE_FACE_RECOGNITION = "feature_face_recognition";
	const FEATURE_FLASH_MODE = "feature_face_mode";
	const FEATURE_CAMERA_ZOOM = "feature_camera_zoom";
	const FEATURE_COLOR_EFFECT = "feature_color_effect";
	const FEATURE_SCENE_MODE = "feature_scene_mode";
	const FEATURE_WHITE_BALANCE = "feature_white_balance";


#	const PARAMETER_IMAGE_QUALITY = "param_image_quality";
	const PARAMETER_IMAGE_RESOLUTION = "param_image_resolution";
	const PARAMETER_FLASH_MODE = "param_flash_mode";
#	const PARAMETER_ENABLE_SHUTTER_SOUND = "param_enable_shutter_sound";
	const PARAMETER_PITCH_TO_ZOOM = "param_pitch_to_zoom";
	const PARAMETER_FACE_RECOGNITION = "param_face_recognition";
	const PARAMETER_FACE_RECOGNITION_BOUND_COLOR = "param_face_recognition_bound_color";
	const PARAMETER_FACE_RECOGNITION_BOUND_SHAPE = "param_face_recognition_bound_shape";
	const PARAMETER_FACE_RECOGNITION_BOUND_LINE_SIZE = "param_face_recognition_bound_line_size";
	const PARAMETER_SCENE_MODE = "param_scene_mode";
	const PARAMETER_WHITE_BALANCE = "param_white_balance";
	const PARAMETER_COLOR_EFFECT = "param_color_effect";

	var __plugin__: Object setget __set_blank__, __get_blank__;
	var __node__: Control setget __set_blank__, __get_blank__;

	var __camera_features__: Dictionary = {};

	func has_feature(feature: String):
		return __camera_features__.has(feature);
		pass

	func _init(plugin: Object, node):
		__plugin__ = plugin;
		__node__ = node;
		pass

	func _set_camera_features_(features: String):
		__camera_features__ = ParameterSerializer.unserialize(features);
		__node__.emit_signal("initialized");
		resize_view(__node__.get_global_rect()); # HACK-FIX - for some reason camera view is fullscreen when view control is placed in TextureRect (non container node)
		pass

	func _on_picture_taken_(camera_error_code, data, detectedFacesExtra):
		var _detectedFacesExtra = ParameterSerializer.unserialize(detectedFacesExtra);

		if camera_error_code != __node__.OK:
			__node__.emit_signal("picture_taken", camera_error_code, null, null);
		else:
			var image : Image = Image.new();
			var err = image.load_jpg_from_buffer(data);
	
			if err != OK:
				print("An error occured while loading camera image texture!");
	
			var texture = ImageTexture.new();
			texture.create_from_image(image);
	
			__node__.emit_signal("picture_taken", camera_error_code, texture,
				{
					"faces": _detectedFacesExtra.values()
				});
		__plugin__.refreshCameraPreview(get_instance_id());
		pass

	func set_camera_facing(back_facing: bool):
		__plugin__.setPreviewCameraFacing(get_instance_id(), back_facing);
		pass

	func set_parameter(what: String, value):
		if value is int:
			__plugin__.setViewParameterInt(get_instance_id(), what, value);
		elif value is bool:
			__plugin__.setViewParameterBool(get_instance_id(), what, value);
		elif value is String:
			__plugin__.setViewParameterString(get_instance_id(), what, value);
		else:
			prints("set_parameter: It's a non supported type");
		pass

	func resize_view(rect: Rect2):
		__plugin__.resizeView(get_instance_id(),
			int(rect.position.x), int(rect.position.y),
				int(rect.size.x), int(rect.size.y));
		pass

	func set_view_visibilty(visible: bool):
		__plugin__.setViewVisibility(get_instance_id(), visible);
		pass

	func destroy_view():
		__plugin__.destroyView(get_instance_id());
		pass

	func take_picture(minimum_number_of_face):
		__plugin__.takePicture(get_instance_id(), minimum_number_of_face);
		pass

	func __set_blank__(value):
		# do nothing...
		pass
	
	func __get_blank__():
			# do nothing...
		pass

const SINGLETON_NAME = "FunababCameraPlugin";
const ParameterSerializer = preload("parameter_serializer.gd");

static func initialize(node: Control)->CameraViewNativeBridge:
	var instanse_id = node.get_instance_id();
	if Engine.has_singleton(SINGLETON_NAME):
		var plugin = Engine.get_singleton(SINGLETON_NAME);
		var r : Rect2= node.get_global_rect();

		var cameraViewNativeBridge = CameraViewNativeBridge.new(plugin, node);
		var result = plugin.initializeView(cameraViewNativeBridge.get_instance_id(),
			node.get_camera_facing(), ParameterSerializer.serialize(node.__get_parameters__()),
				int(r.position.x), int(r.position.y),
					int(r.size.x), int(r.size.y), node.visible);

		if result == true:
			return cameraViewNativeBridge;
		else:
			return null;
	return null;
	pass
