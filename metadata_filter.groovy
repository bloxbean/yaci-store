// Set up a consumer on the Event Bus
vertx.eventBus().consumer("plugin.filterPlugin") { message ->
    println "Received message: ${message.body()}"

    // Process the message and reply
    def result = processMessage(message.body())
    message.reply(result)
}

// Example processing function
def processMessage(data) {
    return data + " processed by Groovy"
}
