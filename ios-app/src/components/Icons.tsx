import React from 'react';
import Ionicons from 'react-native-vector-icons/Ionicons';

export type IconName =
  | 'scan'
  | 'download'
  | 'upload'
  | 'flash'
  | 'star'
  | 'starOutline'
  | 'folder'
  | 'trash'
  | 'lock'
  | 'clock'
  | 'plus'
  | 'play'
  | 'close'
  | 'pencil'
  | 'gear'
  | 'dots'
  | 'chevronRight'
  | 'wifi'
  | 'link'
  | 'card'
  | 'person'
  | 'mail'
  | 'location'
  | 'globe'
  | 'shareUp'
  | 'copy'
  | 'checkmark'
  | 'alert'
  | 'nfc'
  | 'document'
  | 'call'
  | 'chat'
  | 'video';

const ICON_MAP: Record<IconName, string> = {
  scan: 'scan-outline',
  download: 'download-outline',
  upload: 'cloud-upload-outline',
  flash: 'flash-outline',
  star: 'star',
  starOutline: 'star-outline',
  folder: 'folder-outline',
  trash: 'trash-outline',
  lock: 'lock-closed-outline',
  clock: 'time-outline',
  plus: 'add',
  play: 'play',
  close: 'close',
  pencil: 'create-outline',
  gear: 'settings-outline',
  dots: 'ellipsis-horizontal',
  chevronRight: 'chevron-forward',
  wifi: 'wifi-outline',
  link: 'link-outline',
  card: 'card-outline',
  person: 'person-outline',
  mail: 'mail-outline',
  location: 'location-outline',
  globe: 'globe-outline',
  shareUp: 'share-outline',
  copy: 'copy-outline',
  checkmark: 'checkmark-circle',
  alert: 'alert-circle-outline',
  nfc: 'radio-outline',
  document: 'document-text-outline',
  call: 'call-outline',
  chat: 'chatbubble-outline',
  video: 'videocam-outline',
};

export function Icon({ name, color, size = 18 }: { name: IconName; color: string; size?: number }) {
  return <Ionicons name={ICON_MAP[name]} color={color} size={size} />;
}
