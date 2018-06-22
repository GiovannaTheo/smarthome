const Mam = require('../lib/mam.client.js')
const IOTA = require('iota.lib.js')
const iota = new IOTA({ provider: process.argv[2] })

let mamState = Mam.init(iota)
let resp = undefined

async function fetch() {

    // Fetch Stream Async

    switch (process.argv[4]) {
    case 'public':
        resp = await Mam.fetch(process.argv[3], 'public', null)
        break
    case 'private':
        resp = await Mam.fetch(process.argv[3], 'private', null)
        break
    case 'restricted':
        resp = await Mam.fetch(process.argv[3], 'restricted', process.argv[5], null)
        break
    default:
        console.log("Unsupported command")
	}

    if (resp.length !== 0) {

	    msg = 
`{
    ITEMS: ` + iota.utils.fromTrytes(resp.messages[0]).replace(/^\s+|\s+$/g, '') + `,
    ROOT:  ` + process.argv[3] + `, 
    NEXTROOT: ` + resp.nextRoot + `
}`

	    console.log(msg)

    }

}

    
fetch()
