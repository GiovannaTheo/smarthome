const Mam = require('../lib/mam.client.js')
const IOTA = require('iota.lib.js')
const iota = new IOTA({ provider: `https://nodes.testnet.iota.org:443` })

// Init State
let root = 'SVMKV9GQF9QYFPEAAIVYHROYQTEHWGXTEPLUEXKTNEITAZMOARYLDPDLHUXVHLWPUHEYKOVOTVWATABSY'

// Initialise MAM State
let mamState = Mam.init(iota)

// Publish to tangle
const publish = async packet => {
    const message = Mam.create(mamState, packet)
    mamState = message.state
    await Mam.attach(message.payload, message.address)
    return message.root
}

const logData = data => console.log(JSON.parse(iota.utils.fromTrytes(data)))

const execute = async () => {
    // Publish and save root.
    // root = await publish('POTATOONE')
    // Publish but not save root
    // await publish('POTATOTWO')

    ///////////////////////////////////
    // Fetch the messages syncronously
    const resp = await Mam.fetch(root, 'public', null, logData)
    
}

execute()
