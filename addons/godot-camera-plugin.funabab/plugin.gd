tool
extends EditorPlugin

const ANDROID_MODULE = "org/godotengine/godot/funabab/camera/FunababCameraPlugin";
const CUSTOM_NODE_NAME = "CameraView";

func _enter_tree():
	add_custom_type(CUSTOM_NODE_NAME, "Control", preload("camera_view.gd"), preload("icon_node.png"));
	pass

func _exit_tree():
	remove_custom_type(CUSTOM_NODE_NAME);
	pass

func enable_plugin():
	var modules : String = ProjectSettings.get("android/modules");
	
	if modules.find(ANDROID_MODULE) == -1:
		var split := modules.split(",", false);
		split.append(ANDROID_MODULE);
		ProjectSettings.set("android/modules", split.join(","));
	pass

func disable_plugin():
	var modules : String = ProjectSettings.get("android/modules");

	var find = modules.find(ANDROID_MODULE);
	if find != -1:
		var split := modules.split(",", false);
		var part = modules.left(find);
		split.remove(0 if split.size() == 1 else modules.left(find + 1).split(",", false).size() - 1);
		ProjectSettings.set("android/modules", split.join(","));
	pass

