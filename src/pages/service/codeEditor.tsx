// import AceEditor from 'react-ace';
// import 'ace-builds/src-noconflict/mode-javascript';
// import 'ace-builds/src-noconflict/theme-github';
// import 'ace-builds/src-noconflict/ext-language_tools'; // 引入语言工具扩展
// import 'ace-builds/src-noconflict/worker-javascript';
import { useState, useImperativeHandle, forwardRef, useEffect } from 'react';
import CodeMirror from '@uiw/react-codemirror';
// import { langs } from '@uiw/codemirror-extensions-langs';
import { StreamLanguage } from '@codemirror/language';
import { shell } from '@codemirror/legacy-modes/mode/shell';
import { yaml } from '@codemirror/legacy-modes/mode/yaml';

interface Props {
	data: string;
}
const CodeEditor = forwardRef<{ handleSave: () => string } | null, Props>(({ data }, ref) => {
	const [value, setValue] = useState(data);
	const handleChange = (val: string) => {
		setValue(val);
	};
	const handleSave = () => {
		return value;
	};
	useImperativeHandle(ref, () => ({
		handleSave
	}));
	useEffect(() => {
		setValue(data);
	}, [data]);

	return (
		<CodeMirror
			value={value}
			height="500px"
			extensions={[StreamLanguage.define(shell), StreamLanguage.define(yaml)]}
			onChange={handleChange}
		/>
	);
});

export default CodeEditor;