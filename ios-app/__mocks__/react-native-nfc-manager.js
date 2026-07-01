/**
 * Manual Jest mock for react-native-nfc-manager. There's no NFC hardware (or native module)
 * in the Jest/JSDOM-less test environment, so the real package's eager NativeEventEmitter
 * construction throws on import. This stub reproduces just the surface our code calls,
 * enough for component/unit tests that don't specifically exercise NFC I/O.
 */

const NfcTech = {
  Ndef: 'Ndef',
  NfcA: 'NfcA',
  NfcB: 'NfcB',
  NfcF: 'NfcF',
  NfcV: 'NfcV',
  IsoDep: 'IsoDep',
  MifareClassic: 'MifareClassic',
  MifareUltralight: 'MifareUltralight',
  MifareIOS: 'mifare',
  Iso15693IOS: 'iso15693',
  FelicaIOS: 'felica',
  NdefFormatable: 'NdefFormatable',
};

function bytesToString(bytes) {
  return Array.isArray(bytes) ? String.fromCharCode(...bytes) : String(bytes);
}

function stringToBytes(str) {
  return Array.from(str).map((c) => c.charCodeAt(0));
}

const Ndef = {
  TNF_EMPTY: 0x0,
  TNF_WELL_KNOWN: 0x01,
  TNF_MIME_MEDIA: 0x02,
  TNF_ABSOLUTE_URI: 0x03,
  TNF_EXTERNAL_TYPE: 0x04,
  TNF_UNKNOWN: 0x05,
  TNF_UNCHANGED: 0x06,
  TNF_RESERVED: 0x07,
  RTD_TEXT: 'T',
  RTD_URI: 'U',
  MIME_WFA_WSC: 'application/vnd.wfa.wsc',
  util: { bytesToString, stringToBytes, bytesToHexString: bytesToString, toHex: (i) => i, toPrintable: (i) => String(i) },
  text: {
    decodePayload: () => '',
    encodePayload: () => ({ tnf: 0x01, type: [0x54], id: [], payload: [] }),
  },
  uri: {
    decodePayload: () => '',
    encodePayload: (uri) => ({ tnf: 0x01, type: [0x55], id: [], payload: stringToBytes(uri) }),
  },
  wifiSimple: {
    decodePayload: () => ({ ssid: '', networkKey: '' }),
    encodePayload: () => ({ tnf: 0x02, type: stringToBytes('application/vnd.wfa.wsc'), id: [], payload: [] }),
  },
  isType: () => false,
  stringify: () => '',
  encodeMessage: () => [],
  decodeMessage: () => [],
  textRecord: (text) => ({ tnf: 0x01, type: [0x54], id: [], payload: stringToBytes(text) }),
  uriRecord: (uri) => ({ tnf: 0x01, type: [0x55], id: [], payload: stringToBytes(uri) }),
  wifiSimpleRecord: () => ({ tnf: 0x02, type: stringToBytes('application/vnd.wfa.wsc'), id: [], payload: [] }),
  androidApplicationRecord: (pkg) => ({ tnf: 0x04, type: stringToBytes('android.com:pkg'), id: [], payload: stringToBytes(pkg) }),
  record: (tnf, type, id, payload) => ({
    tnf,
    type: typeof type === 'string' ? stringToBytes(type) : type,
    id,
    payload: typeof payload === 'string' ? stringToBytes(payload) : payload,
  }),
};

const NfcError = {
  NfcErrorBase: class NfcErrorBase extends Error {},
};

const nfcManager = {
  start: jest.fn(() => Promise.resolve()),
  isSupported: jest.fn(() => Promise.resolve(false)),
  isEnabled: jest.fn(() => Promise.resolve(false)),
  requestTechnology: jest.fn(() => Promise.resolve(null)),
  cancelTechnologyRequest: jest.fn(() => Promise.resolve()),
  getTag: jest.fn(() => Promise.resolve(null)),
  setEventListener: jest.fn(),
  registerTagEvent: jest.fn(() => Promise.resolve()),
  unregisterTagEvent: jest.fn(() => Promise.resolve()),
  ndefHandler: {
    writeNdefMessage: jest.fn(() => Promise.resolve()),
    getNdefMessage: jest.fn(() => Promise.resolve(null)),
    makeReadOnly: jest.fn(() => Promise.resolve()),
    getNdefStatus: jest.fn(() => Promise.resolve({ status: 2, capacity: 0 })),
  },
};

module.exports = nfcManager;
module.exports.default = nfcManager;
module.exports.NfcTech = NfcTech;
module.exports.Ndef = Ndef;
module.exports.NfcError = NfcError;
