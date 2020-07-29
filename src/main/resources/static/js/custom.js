$(document).ready(function(){

    var exampleInput = $("#code-example");
    var editor = ace.edit("editor");
    var beautify = ace.require("ace/ext/beautify");
    var jsbOpts = {
        indent_size : 2
    };

    editor.setTheme("ace/theme/monokai");
    editor.getSession().setMode("ace/mode/java");
    editor.getSession().setValue(exampleInput.val());
    beautify.beautify(editor.session);

    $("#submit-btn").on('click', function(event) {
        event.preventDefault();

        $("#submit-btn").attr("disabled", true);
        $(".compiling-message-process").removeClass("d-none");
        $(".compiling-message-result").addClass("d-none");
        $("#compile-output").html("");

        $("#code").val(ace.edit('editor').getSession().getValue());

        const data = {};
        $("#main-form").serializeArray().forEach(function(obj) {
            data[obj.name] = obj.value;
        });
        $.ajax({
            url: "/compile",
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify(data),
            success: function(data){
                $(".compiling-message-process").addClass("d-none");
                $(".compiling-message-result").removeClass("d-none");
                $("#submit-btn").removeAttr("disabled");
                $("#compile-output").html(data);
            },
            error: function (error) {
                console.log(error);
            }
        });
    });
});