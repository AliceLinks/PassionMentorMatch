const api = require('../../utils/request.js');
const app = getApp();

Page({
  data: {
    weekStart: '', // 本周一日期 YYYY-MM-DD
    displayDates: [], // 顶部日历显示
    courses: [],
    filteredCourses: [],
    selectedDate: '',
    loading: false
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 1 });
    }
    // 移除手动高亮 tabBar
    this.initWeek();
    this.fetchCourses();
  },

  // 初始化本周日期
  initWeek(baseDate) {
    const now = baseDate ? new Date(baseDate) : new Date();
    const day = now.getDay() || 7; // 周日为0，改为7
    const monday = new Date(now);
    monday.setDate(now.getDate() - day + 1);

    const weekStart = this.formatDate(monday);

    // 生成周一到周日的显示数据
    const displayDates = [];
    for (let i = 0; i < 7; i++) {
      const d = new Date(monday);
      d.setDate(monday.getDate() + i);
      displayDates.push({
        weekDay: ['日', '一', '二', '三', '四', '五', '六'][d.getDay()],
        date: d.getDate(),
        fullDate: this.formatDate(d)
      });
    }

    // 默认选中“今天”，若今天不在本周，仍然设为今天以便后续筛选为空
    const todayStr = this.formatDate(new Date());
    this.setData({ weekStart, displayDates, selectedDate: todayStr });
  },

  formatDate(date) {
    const y = date.getFullYear();
    const m = (date.getMonth() + 1).toString().padStart(2, '0');
    const d = date.getDate().toString().padStart(2, '0');
    return `${y}-${m}-${d}`;
  },

  // 切换到上一周
  onPrevWeek() {
    const cur = new Date(this.data.weekStart);
    cur.setDate(cur.getDate() - 7);
    const prevMondayStr = this.formatDate(cur);
    this.initWeek(prevMondayStr);
    this.fetchCourses();
  },

  // 切换到下一周
  onNextWeek() {
    const cur = new Date(this.data.weekStart);
    cur.setDate(cur.getDate() + 7);
    const nextMondayStr = this.formatDate(cur);
    this.initWeek(nextMondayStr);
    this.fetchCourses();
  },

  fetchCourses() {
    this.setData({ loading: true });
    api.get('/api/courses/week', { week_start: this.data.weekStart })
      .then(res => {
        const list = (res.courses || []).map(item => {
          const d = new Date(item.course_date);
          const wd = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()];
          // 强制将所有id转为字符串，避免大整数精度丢失
          return { ...item, id: String(item.id), reservation_id: item.reservation_id ? String(item.reservation_id) : undefined, weekday: wd };
        });
        // 若已有选中日期，则按选中日期进行默认筛选
        const sel = this.data.selectedDate;
        const filtered = sel ? list.filter(c => c.course_date === sel) : [];
        this.setData({ courses: list, filteredCourses: filtered });
      })
      .finally(() => this.setData({ loading: false }));
  },

  onSelectDay(e) {
    const date = e.currentTarget.dataset.date;
    const filtered = (this.data.courses || []).filter(c => {
      // c.course_date 可能是 yyyy-MM-dd 字符串
      return c.course_date === date;
    });
    this.setData({ filteredCourses: filtered, selectedDate: date });
  },

  onBook(e) {
    let courseId = e.currentTarget.dataset.id;
    courseId = String(courseId);
    wx.showModal({
      title: '预约确认',
      content: '确定要预约这节导师课吗？',
      success: (res) => {
        if (res.confirm) {
          api.post('/api/reservations', { course_id: courseId })
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
  },

  onCancel(e) {
    let reservationId = e.currentTarget.dataset.id;
    if (!reservationId) return;
    reservationId = String(reservationId);
    wx.showModal({
      title: '取消预约',
      content: '确定要取消该课程预约吗？',
      success: (r) => {
        if (r.confirm) {
          api.del(`/api/reservations/${reservationId}`)
            .then(() => { wx.showToast({ title: '已取消' }); this.fetchCourses(); })
            .catch(() => { wx.showToast({ title: '取消失败', icon: 'none' }); });
        }
      }
    });
  },
});