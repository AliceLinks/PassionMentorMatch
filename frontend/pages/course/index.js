const api = require('../../utils/request.js');
const app = getApp();

Page({
  data: {
    weekStart: '',
    displayDates: [],
    courses: [],
    loading: false
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 0 });
    }
    this.initWeek();
    const token = wx.getStorageSync('token');
    if (token) {
      this.fetchCourses();
    } else {
      this.setData({ courses: [] });
    }
  },

  // 初始化本周日期
  initWeek() {
    const now = new Date();
    const day = now.getDay() || 7; 
    const monday = new Date(now);
    monday.setDate(now.getDate() - day + 1);
    
    const weekStart = this.formatDate(monday);
    
    // 生成周一到周日的显示数据
    const displayDates = [];
    for(let i=0; i<7; i++) {
      const d = new Date(monday);
      d.setDate(monday.getDate() + i);
      displayDates.push({
        weekDay: ['日','一','二','三','四','五','六'][d.getDay()],
        date: d.getDate(),
        fullDate: this.formatDate(d)
      });
    }

    this.setData({ weekStart, displayDates });
  },

  formatDate(date) {
    const y = date.getFullYear();
    const m = (date.getMonth() + 1).toString().padStart(2, '0');
    const d = date.getDate().toString().padStart(2, '0');
    return `${y}-${m}-${d}`;
  },

  fetchCourses() {
    this.setData({ loading: true });
    api.get('/courses/week', { week_start: this.data.weekStart })
      .then(res => {
        this.setData({ courses: res.courses || [] });
      })
      .finally(() => this.setData({ loading: false }));
  },

  onBook(e) {
    const courseId = e.currentTarget.dataset.id;
    wx.showModal({
      title: '预约确认',
      content: '确定要预约这节导师课吗？',
      success: (res) => {
        if (res.confirm) {
          api.post('/reservations', { course_id: courseId })
            .then(() => {
              wx.showToast({ title: '预约成功', icon: 'success' });
              this.fetchCourses(); // 刷新状态
            })
            .catch(err => {
              // 错误处理：403无卡，409已满
              if (err.code === 403) {
                wx.showModal({ title: '无法预约', content: '您当前没有有效的导师卡', showCancel: false });
              } else if (err.code === 409) {
                wx.showModal({ title: '预约失败', content: err.message || '课程已满或时间冲突', showCancel: false });
              }
            });
        }
      }
    });
  }
});