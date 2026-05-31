export const quickCategories = ['证件卡片', '数码产品', '钥匙挂件', '书籍文具', '衣物鞋帽', '生活用品', '其他物品']

export const placeOptions = ['图书馆', '食堂', '教学楼', '操场', '宿舍区', '实验楼', '快递站', '校门口']

export const latestLostItems = [
  { name: '校园卡', place: '图书馆', time: '2026-04-21', status: '待寻回' },
  { name: '蓝牙耳机', place: '教学楼', time: '2026-04-20', status: '待寻回' },
]

export const latestFoundItems = [
  { name: '黑色雨伞', place: '食堂', time: '2026-04-21', status: '待认领' },
  { name: '保温杯', place: '操场', time: '2026-04-20', status: '待认领' },
]

export const publicItems = [
  { name: '校园卡', type: '遗失', category: '证件卡片', place: '图书馆', time: '2026-04-21', similarity: '95%', status: '待寻回' },
  { name: '黑色雨伞', type: '招领', category: '生活用品', place: '食堂', time: '2026-04-21', similarity: '91%', status: '待认领' },
  { name: '蓝牙耳机', type: '遗失', category: '数码产品', place: '教学楼', time: '2026-04-20', similarity: '88%', status: '待寻回' },
]

export const personalSections = [
  '个人资料修改',
  '我的遗失发布',
  '我的招领发布',
  '我的认领申请',
  '待我确认的认领',
  '系统消息通知',
]

export const reviewPendingItems = [
  { id: 1, title: '校园卡', type: '遗失物品', publisher: '用户XXX', time: '2026-04-21' },
  { id: 2, title: '黑色雨伞', type: '招领物品', publisher: '用户YYY', time: '2026-04-21' },
]

export const reviewHistoryItems = [
  { id: 1, title: '校园卡', result: '审核通过', time: '2026-04-21', admin: '审核管理员', reason: '-' },
  { id: 2, title: '保温杯', result: '审核驳回', time: '2026-04-21', admin: '审核管理员', reason: '信息违规' },
]

export const adminStats = [
  { label: '物品总数', value: '2,568' },
  { label: '待审核', value: '128' },
  { label: '已认领', value: '976' },
  { label: '今日新增', value: '36' },
]

export const adminLostRows = [
  { id: 1, name: '校园卡', category: '证件', place: '图书馆', user: '用户A', status: '已通过' },
]

export const adminFoundRows = [
  { id: 1, name: '黑色雨伞', category: '杂物', place: '食堂', user: '用户B', status: '已通过' },
]

export const adminPendingRows = [
  { id: 1, title: '蓝牙耳机', type: '遗失', user: '用户C', time: '2026-04-21' },
]

export const claimRows = [
  { id: 1, name: '校园卡', applicant: '用户D', publisher: '用户A', time: '2026-04-21', status: '待确认' },
]

export const userRows = [
  { id: 1, username: 'user1', phone: '138xxxx', time: '2026-04', status: '正常' },
]

export const announcementRows = [
  { id: 1, title: '系统使用须知', time: '2026-04-01', status: '已发布' },
]

export const adminAccountRows = [
  { role: '审核管理员', account: 'admin2', permission: '仅物品审核', time: '2026-04-01' },
]

export const reviewMenu = [
  { label: '待审核物品列表', to: '/review-pending' },
  { label: '已审核历史记录', to: '/review-history' },
]

export const adminMenu = [
  { label: '系统首页', to: '/admin-dashboard' },
  { label: '遗失物品管理', to: '/admin-lost-items' },
  { label: '招领物品管理', to: '/admin-found-items' },
  { label: '待审核物品管理', to: '/admin-pending-items' },
  { label: '认领记录管理', to: '/admin-claims' },
  { label: '用户信息管理', to: '/admin-users' },
  { label: '公告信息管理', to: '/admin-announcements' },
  { label: '管理员账号管理', to: '/admin-accounts' },
]
