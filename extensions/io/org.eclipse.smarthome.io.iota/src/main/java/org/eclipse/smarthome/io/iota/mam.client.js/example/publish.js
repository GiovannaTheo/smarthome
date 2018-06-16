/*
*
* Call this script this way:
*
* If you're sending the first message, you need to initialize the root for all messages:
*   node publish.js "protocol://host:port" "json object" "mode"
*
* Mode can be of three types: public, private or restrited. If restricted is chosen, you have to add 
* as argument the key you want to use:
*   node publish.js "protocol://host:port" "json object" "restricted" "key"
*
* If you're sending a subsequent message of the first one:
*   node publish.js "protocol://host:port" "json object" "mode" "seed" x
* For restricte mode:
*   node publish.js "protocol://host:port" "json object" "restricted" "key" seed" x
*
*   where x is an int defining the number of the message (1st message, 2nd one, etc)
*   x should start with 1
*
* Example:
*
*   node publish.js "https://nodes.testnet.iota.org:443" "json object" "public"
*   node publish.js "https://nodes.testnet.iota.org:443" "json object" "public" "seed" 1
*   node publish.js "https://nodes.testnet.iota.org:443" "json object" "public" "seed" 2
*   ...
*/


const Mam = require('../lib/mam.client.js')
const IOTA = require('iota.lib.js')
const iota = new IOTA({ provider: process.argv[2] })
let states = iota.utils.toTrytes((process.argv[3]).toUpperCase())

// Initialise MAM State - PUBLIC

let mamState = undefined
let obj = undefined

// Publish to tangle - public mode
async function publishPublic() {

    if (process.argv[5] === undefined) {
        mamState = Mam.init(iota)
        let message = Mam.create(mamState, states)
        mamState = message.state

        obj = await Mam.attach(message.payload, message.address)

        if (obj.length !== 0) {

            msg = 
`{ 
    "SEED": "` + mamState['seed'] + `" ,
    "ROOT": "` + message.root + `", 
    "NEXTROOT": "` + mamState['channel']['next_root'] + `",
    "START": "` + mamState['channel']['start'] + `"
}`

        console.log(msg)
        
        }   
    } else {
        mamState = Mam.init(iota, process.argv[5])
        let message = Mam.create(mamState, states)
        mamState = message.state
        let start = parseInt(process.argv[6])

        for (let i=0; i < start; i++){
            message = Mam.create(mamState, states)
            mamState = message.state
        }    

        obj = await Mam.attach(message.payload, message.address)

        if (obj.length !== 0) {
            
            msg = 
`{ 
    "SEED": "` + mamState['seed'] + `" ,
    "ROOT": "` + message.root + `", 
    "NEXTROOT": "` + mamState['channel']['next_root'] + `",
    "START": "` + mamState['channel']['start'] + `"
}`

        console.log(msg)
        
        }
    }
}

// Publish to tangle - private mode
async function publishPrivate() {

    if (process.argv[5] === undefined) {
        mamState = Mam.init(iota)
        mamState = Mam.changeMode(mamState, 'private')   

        let message = Mam.create(mamState, states)
        mamState = message.state

        obj = await Mam.attach(message.payload, message.address)

        if (obj.length !== 0) {

            msg = 
`{ 
    "SEED": "` + mamState['seed'] + `" ,
    "ROOT": "` + message.root + `", 
    "ADDRESS": "` + message.address + `",
    "NEXTROOT": "` + mamState['channel']['next_root'] + `",
    "START": "` + mamState['channel']['start'] + `"
}`

        console.log(msg)
        
        }

    } else {
        mamState = Mam.init(iota, process.argv[5])
        mamState = Mam.changeMode(mamState, 'private')  

        let message = Mam.create(mamState, states)
        mamState = message.state 

        let start = parseInt(process.argv[6])

        for (let i=0; i < start; i++){
            message = Mam.create(mamState, states)
            mamState = message.state
        }    

        obj = await Mam.attach(message.payload, message.address)

        if (obj.length !== 0) {
            
            msg = 
`{ 
    "SEED": "` + mamState['seed'] + `" ,
    "ROOT": "` + message.root + `", 
    "ADDRESS": "` + message.address + `",
    "NEXTROOT": "` + mamState['channel']['next_root'] + `",
    "START": "` + mamState['channel']['start'] + `"
}`

        console.log(msg)
        
        }
    }
    
}

// Publish to tangle - restricted mode
async function publishRestricted() {

    if (process.argv[5] !== undefined){
        if (process.argv[6] === undefined) {
            mamState = Mam.init(iota)
            mamState = Mam.changeMode(
                mamState,
                'restricted',
                process.argv[5].toUpperCase()
            )

            let message = Mam.create(mamState, states)
            mamState = message.state

            obj = await Mam.attach(message.payload, message.address)

            if (obj.length !== 0) {

                msg = 
`{ 
    "SEED": "` + mamState['seed'] + `" ,
    "ROOT": "` + message.root + `", 
    "ADDRESS": "` + message.address + `",
    "NEXTROOT": "` + mamState['channel']['next_root'] + `",
    "START": "` + mamState['channel']['start'] + `"
}`

            console.log(msg)
            
            }

        } else {
            mamState = Mam.init(iota, process.argv[6])
            mamState = Mam.changeMode(
                mamState,
                'restricted',
                process.argv[5].toUpperCase()
            )   

            let message = Mam.create(mamState, states)
            mamState = message.state

            let start = parseInt(process.argv[7])

            for (let i=0; i < start; i++){
                message = Mam.create(mamState, states)
                mamState = message.state
            }    

            obj = await Mam.attach(message.payload, message.address)

            if (obj.length !== 0) {
                
                msg = 
`{ 
    "SEED": "` + mamState['seed'] + `" ,
    "ROOT": "` + message.root + `", 
    "ADDRESS": "` + message.address + `",
    "NEXTROOT": "` + mamState['channel']['next_root'] + `",
    "START": "` + mamState['channel']['start'] + `"
}`

            console.log(msg)
            
            }
        }
    }

}

switch (process.argv[4]) {
    case 'public':
        publishPublic()
        break
    case 'private':
        publishPrivate()
        break
    case 'restricted':
        publishRestricted()
        break
    default:
        console.log("Unsupported command")
}




