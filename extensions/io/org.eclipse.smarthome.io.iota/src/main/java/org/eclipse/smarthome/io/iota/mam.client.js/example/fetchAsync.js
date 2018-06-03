const Mam = require('../lib/mam.client.js')
const IOTA = require('iota.lib.js')
const iota = new IOTA({ provider: process.argv[2] })

let mamState = Mam.init(iota)

async function fetch() {

    // Fetch Stream Async to Test
    const resp = await Mam.fetch(process.argv[3], 'public', null)

    if (resp.length !== 0) {
    	tmp = resp.messages[0].split('9')

	    msg = 
`{ 
    Name: ` + tmp[0] + ` ,
    State: ` + tmp[1] + `,
    Root: ` + process.argv[3] + `, 
    NextRoot: ` + resp.nextRoot + `
}`

	    console.log(msg)

    }

}

    
fetch()
