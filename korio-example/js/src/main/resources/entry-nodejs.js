if (process.env.NODE_PATH == '.') {
	let app = require('./output.js');
	app.com.soywiz.korio.example.MainJs.main();
} else {
	const { spawn } = require('child_process');
	const child = spawn('node', [__filename], { env: { 'NODE_PATH': '.' } });

	child.stdin.pipe(process.stdin);
	child.stdout.pipe(process.stdout);
	child.stderr.pipe(process.stderr);

	//child.stdout.on('data', (data) => { console.log(data);});
	//child.stderr.on('data', (data) => { console.log(data); });
	child.on('close', (code) => { console.log(`child process exited with code ${code}`); });
}