function copy2Clipboard(data) {
	let copyFrom = document.createElement("textarea");
	document.body.appendChild(copyFrom);
	copyFrom.textContent = data;
	copyFrom.select();
	document.execCommand("copy");
	copyFrom.remove();
}

function moveArrayItem(arr, old_index, new_index) {
	if (new_index >= arr.length) {
		var k = new_index - arr.length + 1;
		while (k--) {
			arr.push(undefined);
		}
	}
	arr.splice(new_index, 0, arr.splice(old_index, 1)[0]);
	return arr;
}

function tsv2Table(data) {
	var parsedData=d3.tsvParse(data);
	var tableData = [];
	for (var i = 0; i < parsedData.length; i++) {
		var json = JSON.stringify(parsedData[i]);
		json=json.replace('{', '{"--":'+(i+1)+',');
		tableData.push(JSON.parse(json));
	}

	var table = new Tabulator("#results_table", {
		data:tableData
		, autoColumns:true
		, resizableColumns:false
		, variableHeight:true
		//, layout:"fitColumns"
		//, responsiveLayout:true
		, formatter:"textarea"
		, layout:"fitDataFill"
	});

	var dataMutator=function(value, data, type, params, component) {
		value = value.replace(/\\n/g, "\n").replace(/\\t/g, "\t");
		return value;
		//value - original value of the cell
		//data - the data for the row
		//type - the type of mutation occurring  (data|edit)
		//params - the mutatorParams object from the column definition
		//component - when the "type" argument is "edit", this contains the cell component for the edited cell, otherwise it is the column component for the column

		//return value > mutatorParams.threshold; //return the new value for the cell data.
	}

	var columns = table.getColumnDefinitions();
	var i;
	var counterDone=false;
	for (i = 0; i < columns.length; i++) {
		if(columns[i].title==="--" && !counterDone) {
			columns[i].title="";
			columns[i].frozen=true;
			counterDone=true;
		} else {
			columns[i].formatter="textarea";
//            columns[i].mutator=dataMutator;
//            columns[i].mutatorParams={};
//            columns[i].variableHeight=true;
		}
	}
	if(i<columns.length) {
		columns=moveArrayItem(columns, i, 0);
	}
	table.setColumns(columns);

	document.getElementById("copy_tsv").onclick=
		function() {copy2Clipboard(d3.tsvFormat(parsedData)); };
	document.getElementById("copy_csv").onclick=
		function() {copy2Clipboard(d3.csvFormat(parsedData)); };
	document.getElementById("copy_json").onclick=
		function() {copy2Clipboard(JSON.stringify(parsedData).replace(/},{/g,"},\n{")); };

 /*   window.addEventListener('resize', function(){
		table.redraw(true); //trigger full rerender including all data and rows
	});*/
}

function showResult(data, success) {
	if(!success) {
	  /*  var responseText=data.responseText;
		if(!responseText.startsWith("{")) {
			responseText = {
				response: data.responseText
			}
		}
		var responseObject=JSON.parse(responseText);*/
		data="Error\n"+/*"Error\t\nHTTP Status\t"+
			+data.status.toString()+"\n"
			+*/flatObjectString(data)
//            +"Response: \""+data.responseText.replace(/\n/g," ")
//                .replace(/"/g,"\\\"")+"\”\n"
			;
	}
	window.results.setValue(data, -1);
	var main_grid = document.getElementById("main_grid");
	if(success) {
		if(data.trim().length == 0) {
			data+="\nNo results found";
		} else if(data.trim().indexOf("\n")<0) {
			data=data.trim()+"\n"+data.trim().replace(/[^\t]*/g,"");
		}
		document.getElementById("results").style.display = "none";
		document.getElementById("results_table").style.display = "block";
		tsv2Table(data);
	} else {
		document.getElementById("results_table").style.display = "none";
		document.getElementById("results").style.display = "block";
//        document.getElementById("results").style.display = "none";
//        document.getElementById("results_table").style.display = "block";
		//tsv2Table(data);
	}
}

function flatObjectString(obj) {
	var result="";
	Object.keys(obj).forEach(key=>{
	   var value=obj[key];
	   if((typeof value) == "string" && key != "statusText") {
			value=value
			    .replace(/\\n/g, "\n")
			    .replace(/\\t/g, "\t")
			    .replace(/^ERROR: /, "")
			    ;
			if(key != "responseText") {
				result+=`${key}:\t`
			}
			result+=`${value}\n`;
	   }
	});
	return result.trim();
}

function postQuery(query) {
	if(query.trim().length > 0) {
		$.post('/sql',{query:query})
		.done(function(data){ showResult(data, true); })
		.fail(function(data){ showResult(data, false); });
	}
}

function runQuery() {
	postQuery(window.editor.getValue());
	return false;
}

function runSelectedQuery() {
	var selectedText=window.editor.getSelectedText();
	if(selectedText.trim().length == 0) {
		var selectedLine = editor.getSelectionRange().start.row;
		selectedText = editor.session.getLine(selectedLine);
	}
	postQuery(selectedText);
	return false;
}

function createEditor() {
	//good themes: terminal, chaos, clouds_midnight, monokai
	var editor = ace.edit("query", {
		theme: "ace/theme/terminal",
		mode: "ace/mode/sql",
		minLines: 50,
		showPrintMargin: false
	});

	editor.session.setUseWrapMode(true);

	editor.focus();
	editor.gotoLine(1);
	editor.commands.addCommand({
		name: 'run',
		bindKey: {
			win: 'Alt-R',
			mac: 'Option-R',
			sender: 'editor|cli'
		},
		exec: function() { runSelectedQuery(); return true; }
	});
	return editor;
}

function createResultView() {
	var results=ace.edit("results");
		results.setTheme("ace/theme/terminal");
		results.session.setMode("ace/mode/typescript");
		results.session.setUseWrapMode(true);
		results.setReadOnly(true);
		results.setOptions({readOnly: true, highlightActiveLine: false, highlightGutterLine: false});
		results.renderer.$cursorLayer.element.style.display = "none";
		results.setShowPrintMargin(false);
	return results
}

function createSplit() {
	Split({
        rowGutters: [{
            track: 1,
            element: document.querySelector('#horizontal-splitter'),
        }]
	});
}

function editorDataKey() {
	var key= "editorData."+location.port;
	return key;
}

function loadEditorData() {
	if(window.editor) {
		var editorData=JSON.parse(window.localStorage.getItem(editorDataKey()));
		if(editorData) {
			window.editor.setValue(editorData.text, -1);
			window.editor.moveCursorToPosition(editorData.cursorPos);
			window.editor.scrollToLine(editorData.scrollRow, false);
		}
	}
}

function saveEditorData() {
	if(window.editor) {
		var data=JSON.stringify({
			 text: window.editor.getValue(),
			 cursorPos: window.editor.getCursorPosition(),
			 scrollRow: window.editor.getFirstVisibleRow()
		});
		window.localStorage.setItem(editorDataKey(), data);
	}
}

window.onload=function(){
	ace.config.set("basePath", "libs/ace");
	window.results = createResultView();
	window.editor = createEditor();
	loadEditorData();
	//createSplit();
	document.getElementById("submit").onclick=runQuery;
	document.getElementById("submit_selected").onclick=runSelectedQuery;
}

window.onunload=function(){
	saveEditorData();
}
