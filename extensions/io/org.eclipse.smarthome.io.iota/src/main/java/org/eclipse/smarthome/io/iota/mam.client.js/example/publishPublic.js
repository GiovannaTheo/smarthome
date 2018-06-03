/*
*
* Call this script this way:
*
* If you're sending the first message, you need to initialize the root for all messages:
*   node publishPublic.js "protocol://host:port" "itemName" "itemState"
*
* If you're sending a subsequent message of the first one:
*   node publishPublic.js "protocol://host:port" "itemName" "itemState" "seed" x
*
*   where x is an int defining the number of the message (1st message, 2nd one, etc)
*   x should start with 1
*
* Example:
*
*   node publishPublic.js "https://nodes.testnet.iota.org:443" "myName" "myState"
*   node publishPublic.js "https://nodes.testnet.iota.org:443" "myName" "myState1" "seed" 1
*   node publishPublic.js "https://nodes.testnet.iota.org:443" "myName" "myState2" "seed" 2
*   ...
*/


const Mam = require('../lib/mam.client.js')
const IOTA = require('iota.lib.js')
const iota = new IOTA({ provider: process.argv[2] })
const itemName = process.argv[3].toUpperCase()
const itemState = process.argv[4].toUpperCase()

// Initialise MAM State - PUBLIC

let mamState = undefined
let obj = undefined

if (process.argv[5] === undefined) {
    mamState = Mam.init(iota)   
} else {
    mamState = Mam.init(iota, process.argv[5])
}

// Publish to tangle
async function publish() {
    
    let message = Mam.create(mamState, itemName+itemState)
    mamState = message.state

    if (process.argv[5] === undefined) {

        obj = await Mam.attach(message.payload, message.root)

        if (obj.length !== 0) {

            msg = 
`{ 
    Seed: ` + mamState['seed'] + ` ,
    Root: ` + message.root + `, 
    NextRoot: ` + mamState['channel']['next_root'] + `,
    Start: ` + mamState['channel']['start'] + `
}`

        console.log(msg)
        
        }

    } else {

        let start = parseInt(process.argv[6])

        for (let i=0; i < start; i++){
            message = Mam.create(mamState, itemName+itemState)
            mamState = message.state
        }    

        obj = await Mam.attach(message.payload, message.root)

        if (obj.length !== 0) {
            
            msg = 
`{ 
    Seed: ` + mamState['seed'] + ` ,
    Root: ` + message.root + `, 
    NextRoot: ` + mamState['channel']['next_root'] + `,
    Start: ` + mamState['channel']['start'] + `
}`

        console.log(msg)
        
        }

    }
}

publish()

