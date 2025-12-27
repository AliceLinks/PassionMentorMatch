const app = getApp();
const api = require('../../../utils/request.js');
Page({
  data: {
    courses: [],
    loading: false
  },
  onShow() {
    console.log('onShow 触发'); // 新增调试输出
    this.getList();
  },
  getList() {
    console.log('getList 调用'); // 新增调试输出
    this.setData({ loading: true });
    const params = { week_start: this.getWeekStart() };
    api.get('/courses/week', params)
      .then(res => {
        console.log('getList 返回:', res); // 新增调试输出
        this.setData({ courses: res.courses || [] }); // 修正为 res.courses
      })
      .catch((err) => {
        console.log('getList catch:', err); // 新增调试输出
        this.setData({ courses: [] });
      })
      .finally(() => {
        console.log('getList finally'); // 新增调试输出
        this.setData({ loading: false });
      });
  },
  getWeekStart() {
    const now = new Date();
    const day = now.getDay() || 7;
    now.setDate(now.getDate() - day + 1);
    return now.toISOString().slice(0, 10);
  },
  goEdit(e) {
    let id = e.currentTarget.dataset.id;
    id = String(id);
    wx.navigateTo({ url: '/pages/admin/manageEdit/index?id=' + id });
  },
  onCancel(e) {
    let id = e.currentTarget.dataset.id;
    id = String(id);
    console.log('onCancel 课程ID:', id);
    wx.showModal({
      title: '确认取消',
      content: '将取消课程并批量取消预约，继续？',
      success: (r) => {
        if (!r.confirm) return;
        api.post(`/courses/${id}/cancel`, {})
          .then(res => {
            console.log('取消课程成功:', res);
            wx.showToast({ title: '已取消', icon: 'success' });
            this.getList();
          })
          .catch(err => {
            console.log('取消课程失败:', err);
            wx.showToast({ title: err.message || '取消失败', icon: 'none' });
          });
      }
    });
  },
  onEdit(e){
    let id = e.currentTarget.dataset.id;
    id = String(id);
    wx.navigateTo({ url: `/pages/admin/manageEdit/index?id=${id}` });
  },
  onRoster(e){
    let id = e.currentTarget.dataset.id;
    if(!id){ wx.showToast({ title:'缺少课程ID', icon:'none' }); return; }
    id = String(id);
    wx.navigateTo({ url: `/pages/admin/roster/index?courseId=${id}` });
  }
});