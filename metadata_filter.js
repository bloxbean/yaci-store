vertx.eventBus().consumer("plugin.filterPlugin", function (message) {
    const records = message.body();
    //const filteredRecords = records.filter(record => record.value > 10);
    console.log(records)
    message.reply(filteredRecords); // Send back the filtered results
});

console.log("JavaScript Filter Plugin deployed and listening on 'plugin.filterPlugin'.");
