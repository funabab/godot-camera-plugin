extends Reference

static func unserialize(val: String)->Dictionary:
	var result = {};
	var split : PoolStringArray = val.split(";");

	for data in split:
		var data_split : PoolStringArray = data.split("=");
		if data_split.size() != 2:
			continue;

		var key : String = data_split[0].strip_edges();
		var value : String = data_split[1].to_lower().strip_edges();
		
		if value == "on" || value == "off":
			result[key] = value == "on";
		elif value.begins_with("'"):
			result[key] = value.replace("'", "");
		elif value.split(",", false).size() == 4:
			var value_split: PoolStringArray = value.split(",", false);
			var rect: Rect2 = Rect2();
			rect.position.x = int(value_split[0]);
			rect.position.y = int(value_split[1]);
			rect.size.x = int(value_split[2]);
			rect.size.y = int(value_split[3]);

			result[key] = rect;
		else:
			result[key] = int(value);
	
	return result;
	pass

static func serialize(params: Dictionary)->String:
	var buffer: = PoolStringArray();

	for key in params:
		var value = params[key];
		var data;
		if value is String:
			data = "'" + value + "'";
		elif value is bool:
			data = "on" if value else "off";
		elif value is Rect2:
			data = "%d,%d,%d,%d" % [value.position.x, value.position.y, value.size.x, value.size.y];
		elif value is int:
			data = str(value);
		else:
			continue;

		buffer.append(key + "=" + data);
	return buffer.join(";");
	pass
