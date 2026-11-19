import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import api from "../../api/axiosInstance";

// Load initial state from localStorage
const loadState = () => {
  try {
    const token = localStorage.getItem("token");
    const username = localStorage.getItem("username");
    const userId = localStorage.getItem("userId");
    const roles = JSON.parse(localStorage.getItem("roles") || "[]");

    if (token && username) {
      return {
        id: parseInt(userId),
        username,
        roles,
        subscriptions: [],
        isAuthenticated: true,
      };
    }
  } catch (e) {
    console.error("Failed to load auth state", e);
  }

  return {
    id: null,
    username: null,
    roles: [],
    subscriptions: [],
    isAuthenticated: false,
  };
};

export const fetchSubscriptions = createAsyncThunk(
  "user/subscriptions",
  async () => {
    const res = await api.get("/api/subscriptions/subscribed");
    return res.data;
  },
);

const userSlice = createSlice({
  name: "user",
  initialState: loadState(),
  reducers: {
    setUser(state, action) {
      const { id, username, roles } = action.payload;
      state.id = id;
      state.username = username;
      state.roles = roles;
      state.isAuthenticated = true;

      // Persist to localStorage
      localStorage.setItem("userId", id);
      localStorage.setItem("username", username);
      localStorage.setItem("roles", JSON.stringify(roles));
    },
    logout(state) {
      state.id = null;
      state.username = null;
      state.roles = [];
      state.subscriptions = [];
      state.isAuthenticated = false;

      // Clear localStorage
      localStorage.removeItem("token");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("userId");
      localStorage.removeItem("username");
      localStorage.removeItem("roles");
    },
    addSubscription(state, action) {
      state.subscriptions.push({
        categoryId: action.payload,
      });
    },
    removeSubscription(state, action) {
      state.subscriptions = state.subscriptions.filter(
        (sub) => sub.categoryId !== action.payload,
      );
    },
  },
  extraReducers: (builder) => {
    builder.addCase(fetchSubscriptions.fulfilled, (state, action) => {
      state.subscriptions = action.payload;
    });
  },
});

export const { setUser, logout, addSubscription, removeSubscription } = userSlice.actions;
export default userSlice.reducer;
