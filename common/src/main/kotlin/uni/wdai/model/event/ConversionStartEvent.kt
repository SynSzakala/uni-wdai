package uni.wdai.model.event

data class ConversionStartEvent(val id: String, val commandLine: List<String>)