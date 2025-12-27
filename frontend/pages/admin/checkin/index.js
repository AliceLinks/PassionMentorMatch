const app = getApp();
const api = require('../../../utils/admin-request.js');
Page({
  data: {
    code: '',
    result: '',
    loading: false
  },
  onInput(e) {
    this.setData({ code: e.detail.value });
  },
  onScan() {
    wx.scanCode({
      success: res => {
        this.setData({ code: res.result });
        this.onCheckin();
      }
    });
  },
  onCheckin() {
    if (!this.data.code) {
      wx.showToast({ title: '请输入或扫码签到码', icon: 'none' });
      return;
    }
    this.setData({ loading: true });
    api.post('/admin/checkin', { code: this.data.code }).then(res => {
      this.setData({ result: res.data.msg || '核销成功' });
    }).catch(() => {
      this.setData({ result: '核销失败，请重试' });
    }).finally(() => {
      this.setData({ loading: false });
    });
  }
});