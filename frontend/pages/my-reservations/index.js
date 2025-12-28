// pages/my-reservations/index.js
const api = require('../../utils/request.js');

Page({
  data: {
    list: [],
    page: 1,
    page_size: 20,
    total: 0,
    loading: false
  },
  onShow() {
    this.fetchList(true);
  },
  fetchList(reset = false) {
    if (this.data.loading) return;
    const page = reset ? 1 : this.data.page;
    this.setData({ loading: true });
    api.get('/api/reservations/my', { page, page_size: this.data.page_size })
      .then(res => {
        const items = (res && res.data) || [];
        const meta = (res && res.meta) || {};
        this.setData({
          list: reset ? items : [...this.data.list, ...items],
          page: page,
          total: meta.total || items.length
        });
      })
      .finally(() => this.setData({ loading: false }));
  },
  onCancel(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '取消预约',
      content: '确定要取消吗？',
      success: (r) => {
        if (r.confirm) {
          api.del(`/api/reservations/${id}`).then(() => {
            wx.showToast({ title: '已取消' });
            this.fetchList(true);
          });
        }
      }
    });
  },
  onReachBottom() {
    const next = this.data.page + 1;
    if (this.data.list.length >= this.data.total) return;
    this.setData({ page: next }, () => this.fetchList(false));
  },
  onPullDownRefresh() {
    this.fetchList(true);
    wx.stopPullDownRefresh();
  }
})