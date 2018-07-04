const Mam = require('../lib/mam.client.js')
const IOTA = require('iota.lib.js')
const iota = new IOTA({ provider: process.argv[2] })
let states = iota.utils.toTrytes((process.argv[3]).toUpperCase())

// USAGE: node publish.js "https://nodes.testnet.iota.org:443" "json object" "seed" "password"

// Initialise MAM State - PUBLIC

let mamState = undefined
let obj = undefined

// Publish to tangle - public mode
async function handshake() {
    mamState = Mam.init(iota, process.argv[4])
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
    "NEXTROOT": "` + mamState['channel']['next_root'] + `",
    "START": "` + mamState['channel']['start'] + `"
}`

    console.log(msg)
    
    }   
}

handshake()




