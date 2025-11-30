Component({
  data: {
    selected: 0,
    list: [
      { pagePath: "/pages/course/index", text: "约课", icon: "📅" },
      { pagePath: "/pages/profile/index", text: "我的", icon: "👤" }
    ]
  },
  methods: {
    onSwitch(e) {
      const idx = Number(e.currentTarget.dataset.index);
      const item = this.data.list[idx];
      wx.switchTab({ url: item.pagePath });
    }
  }
})
