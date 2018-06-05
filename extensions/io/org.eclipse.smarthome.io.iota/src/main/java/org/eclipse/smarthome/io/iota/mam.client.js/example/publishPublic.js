/*
*
* Call this script this way:
*
* If you're sending the first message, you need to initialize the root for all messages:
*   node publishPublic.js "protocol://host:port" "json object"
*
* If you're sending a subsequent message of the first one:
*   node publishPublic.js "protocol://host:port" "json object" "seed" x
*
*   where x is an int defining the number of the message (1st message, 2nd one, etc)
*   x should start with 1
*
* Example:
*
*   node publishPublic.js "https://nodes.testnet.iota.org:443" "json object"
*   node publishPublic.js "https://nodes.testnet.iota.org:443" "json object" "seed" 1
*   node publishPublic.js "https://nodes.testnet.iota.org:443" "json object" "seed" 2
*   ...
*/


const Mam = require('../lib/mam.client.js')
const IOTA = require('iota.lib.js')
const iota = new IOTA({ provider: process.argv[2] })
let states = iota.utils.toTrytes((process.argv[3].substring(1, process.argv[3].length - 1)).toUpperCase())

// Initialise MAM State - PUBLIC

let mamState = undefined
let obj = undefined

if (process.argv[4] === undefined) {
    mamState = Mam.init(iota)   
} else {
    mamState = Mam.init(iota, process.argv[4])
}

// Publish to tangle
async function publish() {
    
    let message = Mam.create(mamState, states)
    mamState = message.state

    if (process.argv[4] === undefined) {

        obj = await Mam.attach(message.payload, message.root)

        if (obj.length !== 0) {

            msg = 
`{ 
    SEED: ` + mamState['seed'] + ` ,
    ROOT: ` + message.root + `, 
    NEXTROOT: ` + mamState['channel']['next_root'] + `,
    START: ` + mamState['channel']['start'] + `
}`

        console.log(msg)
        
        }

    } else {

        let start = parseInt(process.argv[5])

        for (let i=0; i < start; i++){
            message = Mam.create(mamState, states)
            mamState = message.state
        }    

        obj = await Mam.attach(message.payload, message.root)

        if (obj.length !== 0) {
            
            msg = 
`{ 
    SEED: ` + mamState['seed'] + ` ,
    ROOT: ` + message.root + `, 
    NEXTROOT: ` + mamState['channel']['next_root'] + `,
    START: ` + mamState['channel']['start'] + `
}`

        console.log(msg)
        
        }

    }
}

publish()

