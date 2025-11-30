Page({
  data: { list: [] },
  onShow() {
    const list = wx.getStorageSync('checkins') || []
    this.setData({ list })
  },
  onNavigate(e) {
    const lat = Number(e.currentTarget.dataset.lat)
    const lng = Number(e.currentTarget.dataset.lng)
    if (lat && lng) wx.openLocation({ latitude: lat, longitude: lng, name: '打卡地点' })
  }
})
