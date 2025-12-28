const request = require('../../../utils/request.js');

Page({
  data: {
    weekStart: '',
    courses: [],
    loading: false,
    showReservations: false,
    reservations: [],
    selectedCourse: null,
    filter: 'all',
    weekDays: [],
    selectedDate: ''
  },

  onLoad() {
    this.initWeekStart();
    // week days and initial day's courses will be loaded in initWeekStart
  },

  initWeekStart() {
    const d = new Date();
    const day = d.getDay();
    const offset = (day + 6) % 7; // days since Monday
    const monday = new Date(d);
    monday.setDate(d.getDate() - offset);
    const ws = this.formatDate(monday);
    this.setWeekDays(ws);
  },

  setWeekDays(weekStart) {
    const monday = new Date(weekStart);
    const labels = ['一', '二', '三', '四', '五', '六', '日'];
    const arr = [];
    for (let i = 0; i < 7; i++) {
      const day = new Date(monday);
      day.setDate(monday.getDate() + i);
      const fullDate = this.formatDate(day);
      arr.push({
        fullDate: fullDate,
        weekDay: labels[i], // 用于显示 周X 中的 X
        date: day.getDate() // 日数字，用于显示当天日期
      });
    }
    // default select today if it's in this week, otherwise select Monday
    const todayStr = this.formatDate(new Date());
    let defaultIndex = arr.findIndex(a => a.fullDate === todayStr);
    if (defaultIndex === -1) defaultIndex = 0;
    const selFullDate = arr[defaultIndex].fullDate;
    // show year and month based on the selected day (e.g. 2025年12月)
    const weekLabel = this.formatMonthYear(new Date(selFullDate));
    this.setData({ weekStart, weekDays: arr, selectedDate: selFullDate, weekLabel }, () => this.fetchDayCourses(this.data.selectedDate));
  },

  formatDateCN(d) {
    const y = d.getFullYear();
    const m = (d.getMonth() + 1).toString().padStart(2, '0');
    const day = d.getDate().toString().padStart(2, '0');
    return `${y}年${m}月${day}日`;
  },

  formatMonthYear(d) {
    const y = d.getFullYear();
    const m = (d.getMonth() + 1).toString().padStart(2, '0');
    return `${y}年${m}月`;
  },

  formatDate(d) {
    const y = d.getFullYear();
    const m = (d.getMonth() + 1).toString().padStart(2, '0');
    const day = d.getDate().toString().padStart(2, '0');
    return `${y}-${m}-${day}`;
  },

  prevWeek() {
    const cur = new Date(this.data.weekStart);
    cur.setDate(cur.getDate() - 7);
    const ws = this.formatDate(cur);
    this.setWeekDays(ws);
  },

  nextWeek() {
    const cur = new Date(this.data.weekStart);
    cur.setDate(cur.getDate() + 7);
    const ws = this.formatDate(cur);
    this.setWeekDays(ws);
  },

  // user clicks a day in the top nav
  selectDay(e) {
    const date = e.currentTarget.dataset.date;
    if (!date) return;
    const weekLabel = this.formatMonthYear(new Date(date));
    this.setData({ selectedDate: date, weekLabel }, () => this.fetchDayCourses(date));
  },

  previewAvatar(e) {
    const src = e.currentTarget.dataset.src;
    if (!src) return;
    wx.previewImage({ urls: [src], current: src });
  },

  fetchDayCourses(date) {
    if (!date) return;
    this.setData({ loading: true });
    request.get(`/api/admin/courses/day`, { date })
      .then((resp) => {
        const list = (resp && resp.courses) ? resp.courses : [];
        this.setData({ courses: list });
      })
      .catch(() => { })
      .finally(() => this.setData({ loading: false }));
  },

  openReservations(e) {
    const id = e.currentTarget.dataset.id;
    const course = this.data.courses.find(c => String(c.id) === String(id));
    console.log('openReservations, courseId from button:', id);
    if (!course) {
      wx.showToast({ title: '课程不存在', icon: 'none' });
      return;
    }
    this.setData({ selectedCourse: course, showReservations: true, filter: 'all' }, () => this.fetchReservations());
  },

  closeReservations() {
    this.setData({ showReservations: false, reservations: [], selectedCourse: null });
  },

  changeFilter(e) {
    const f = e.currentTarget.dataset.filter;
    this.setData({ filter: f }, () => this.fetchReservations());
  },

  fetchReservations() {
    const course = this.data.selectedCourse;
    if (!course) return;
    const filter = this.data.filter || 'all';
    this.setData({ loading: true });
    request.get(`/api/admin/courses/${course.id}/reservations`, { checkin_status: filter })
      .then((resp) => {
        const items = resp && resp.data ? resp.data : [];
        this.setData({ reservations: items });
      })
      .catch((err) => {
        console.error('fetchReservations error', err);
        const msg = (err && err.message) || (err && err.msg) || '获取预约失败';
        wx.showToast({ title: msg, icon: 'none' });
      })
      .finally(() => this.setData({ loading: false }));
  },

  doCheckin(e) {
    const id = e.currentTarget.dataset.id;
    console.log('doCheckin id=', id);
    if (!id) {
      wx.showToast({ title: 'reservation id missing', icon: 'none' });
      return;
    }
    request.post(`/api/admin/reservations/${id}/checkin`, {})
      .then(() => {
        wx.showToast({ title: '已签到' });
        this.fetchReservations();
      })
      .catch((err) => {
        console.error('doCheckin error', err);
        const msg = (err && err.message) || (err && err.msg) || (err && err.message) || '签到失败';
        wx.showToast({ title: msg, icon: 'none' });
      });
  },

  doUncheckin(e) {
    const id = e.currentTarget.dataset.id;
    console.log('doUncheckin id=', id);
    if (!id) {
      wx.showToast({ title: 'reservation id missing', icon: 'none' });
      return;
    }
    request.post(`/api/admin/reservations/${id}/uncheckin`, {})
      .then(() => {
        wx.showToast({ title: '已取消签到' });
        this.fetchReservations();
      })
      .catch((err) => {
        console.error('doUncheckin error', err);
        const msg = (err && err.message) || (err && err.msg) || '取消签到失败';
        wx.showToast({ title: msg, icon: 'none' });
      });
  }

});

