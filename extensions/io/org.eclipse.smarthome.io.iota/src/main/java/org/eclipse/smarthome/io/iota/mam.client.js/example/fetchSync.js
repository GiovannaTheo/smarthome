const Mam = require('../lib/mam.client.js')
const IOTA = require('iota.lib.js')
const iota = new IOTA({ provider: process.argv[2] })

// Initialise MAM State
let mamState = Mam.init(iota)

// Publish to tangle

async function fetch () {

    var resp = await Mam.fetch(process.argv[3], 'public')
    
    while (resp.messages.length === 0){
        resp = await Mam.fetch(process.argv[3], 'public')
    }
    
    console.log(
`{
    ITEMS: ` + iota.utils.fromTrytes(resp.messages[0]).replace(/^\s+|\s+$/g, '') + `,
    ROOT:  ` + process.argv[3] + `, 
    NEXTROOT: ` + resp.nextRoot + `
}`)

}

fetch()