if (process.env.NODE_PATH == '.') {
	//console.log(process.env);
	let app = require('./output.js');
	app.com.soywiz.korio.example.MainJs.main();
} else {
	const { spawn } = require('child_process');
	const env2 = Object.create(process.env);
	env2.NODE_PATH = '.';
	const child = spawn(process.argv[0], [__filename], { "env": env2 });

	child.stdin.pipe(process.stdin);
	child.stdout.pipe(process.stdout);
	child.stderr.pipe(process.stderr);

	//child.stdout.on('data', (data) => { console.log(data);});
	//child.stderr.on('data', (data) => { console.log(data); });
	child.on('close', (code) => { console.log(`child process exited with code ${code}`); });
}