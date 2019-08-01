2.1.1
1、增加系统过滤，非linux系统统一当做local debug model处理。
2、增加动态日志（服务端/客户端），发生异常动态将日志提升至debug级别，无异常一段时间后再调回原级别。
	-- [ice.client.sboot./ice.server.sboot./ice.registry.sboot.]dynamic.log.enable = true(default)/false
	-- [ice.client.sboot./ice.server.sboot./ice.registry.sboot.]dynamic.log.time = 10(default)(Minute)
3、调整cliport.txt位置，移动到执行目录同级目录。