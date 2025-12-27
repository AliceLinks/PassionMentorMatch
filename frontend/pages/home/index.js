const api = require('../../utils/request.js');

Page({
  data: {
    logoUrl: 'http://127.0.0.1:8080/upload/logo.png',
    user: wx.getStorageSync('userInfo') || { realName: '游客' },
    cards: [],
    recommend: [],
    stats: { courseCount: 0 },
    roadmapImg: '',
    courseIntro: ''
  },
  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 0 });
    }

    // 日志：页面加载时打印 userInfo
    const userInfo = wx.getStorageSync('userInfo');
    console.log('[home] userInfo from storage:', userInfo);

    // 先确保完成登录，避免未携带令牌导致 401
    const app = getApp();
    Promise.resolve(app && typeof app.doLogin === 'function' ? app.doLogin() : null)
      .catch(() => null) // 忽略登录异常，仍尝试读取，交由后端返回 401 时跳转登录
      .finally(() => {
        // 读取用户信息与卡片
        api.get('/user/profile').then(u => {
          console.log('[home] /user/profile 返回:', u);
          this.setData({ user: u });
          wx.setStorageSync('userInfo', u);
          console.log('[home] setData user:', u, 'realName:', u && u.realName);
        }).catch(() => {});
        api.get('/user/cards').then(cards => this.setData({ cards: cards || [] })).catch(() => {});
      });

    // 拉取本周课程作为推荐
    const monday = this.getWeekStart();
    api.get('/courses/week', { week_start: monday })
      .then(res => {
        const list = (res && res.courses) || [];
        this.setData({
          stats: { courseCount: list.length },
          recommend: list.slice(0, 3) // 取前3条做推荐
        });
      })
      .catch(() => {});

    // 拉取路线图和课程介绍
    api.get('/config').then(cfg => {
      this.setData({
        roadmapImg: cfg.roadmapImg || '',
        courseIntro: cfg.courseIntro || ''
      });
    }).catch(() => {});
  },
  getWeekStart() {
    const now = new Date();
    const day = now.getDay() || 7;
    const monday = new Date(now);
    monday.setDate(now.getDate() - day + 1);
    const y = monday.getFullYear();
    const m = (monday.getMonth() + 1).toString().padStart(2, '0');
    const d = monday.getDate().toString().padStart(2, '0');
    return `${y}-${m}-${d}`;
  },
  goCourse() { wx.switchTab({ url: '/pages/course/index' }); },
  goProfile() { wx.switchTab({ url: '/pages/profile/index' }); },
  onBook(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '预约确认',
      content: '确定预约该课程？',
      success: (r) => {
        if (r.confirm) {
          api.post('/reservations', { course_id: id })
            .then(() => wx.showToast({ title: '预约成功', icon: 'success' }))
            .catch(err => {
              if (err.code === 403) {
                wx.showModal({ title: '无法预约', content: '您没有有效导师卡', showCancel: false });
              } else if (err.code === 409) {
                wx.showModal({ title: '预约失败', content: err.message || '课程已满或时间冲突', showCancel: false });
              } else {
                wx.showToast({ title: '预约失败', icon: 'none' });
              }
            });
        }
      }
    });
  },
  goCourseIntro() {
    const url = encodeURIComponent(this.data.courseIntro);
    wx.navigateTo({
      url: `/pages/webview/index?url=${url}`
    });
  }
});