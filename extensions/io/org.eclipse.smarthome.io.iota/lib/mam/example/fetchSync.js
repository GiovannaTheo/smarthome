const Mam = require('../lib/mam.client.js')
const IOTA = require('iota.lib.js')
const iota = new IOTA({ provider: process.argv[2] })

// Initialise MAM State
let mamState = Mam.init(iota)
var resp = undefined

// Fetch from the tangle

async function fetch () {

    switch (process.argv[4]) {
    case 'public':
        resp = await Mam.fetch(process.argv[3], 'public', null)
        break
    case 'private':
        resp = await Mam.fetch(process.argv[3], 'private', null)
        break
    case 'restricted':
        resp = await Mam.fetch(process.argv[3], 'restricted', process.argv[5].toUpperCase(), null)
        break
    default:
        console.log("Unsupported command")
	}
    
    while (resp.messages.length === 0){
        switch (process.argv[4]) {
	    case 'public':
	        resp = await Mam.fetch(process.argv[3], 'public', null)
	        break
	    case 'private':
	        resp = await Mam.fetch(process.argv[3], 'private', null)
	        break
	    case 'restricted':
	        resp = await Mam.fetch(process.argv[3], 'restricted', process.argv[5].toUpperCase(), null)
	        break
	    default:
	        console.log("Unsupported command")
		}
    }
    
    console.log(
`{
    ITEMS: ` + iota.utils.fromTrytes(resp.messages[0]).replace(/^\s+|\s+$/g, '') + `,
    ROOT:  ` + process.argv[3] + `, 
    NEXTROOT: ` + resp.nextRoot + `
}`)

}

fetch()