const Mam = require('../lib/mam.client.js')
const IOTA = require('iota.lib.js')
const iota = new IOTA({ provider: process.argv[2] })
const seed = process.argv[3]

// Initialise MAM State

let mamState = undefined

// Publish to tangle - public mode
function getNextRoot() {
    mamState = Mam.init(iota, seed)
    let message = Mam.create(mamState, "MESSAGE")
    mamState = message.state

    console.log(mamState['channel']['next_root'])
}

getNextRoot();
