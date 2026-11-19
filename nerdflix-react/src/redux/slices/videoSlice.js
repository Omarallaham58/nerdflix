import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import api from "../../api/axiosInstance";

export const fetchVideos = createAsyncThunk("videos/fetch", async () => {
  const res = await api.get("/api/videos");
  return res.data;
});

// console.log("Checking Thunk:", fetchVideos);

const videoSlice = createSlice({
  name: "videos",
  initialState: {
    data: [],
    loading: false,
  },
  reducers: {
    incrementLocalView(state, action) {
      const v = state.data.find((x) => x.id === action.payload);
      console.log("Incrementing view for video ID:", action.payload);
      if (v) {
        console.log("Current views before increment:", v.stats.views);
        v.stats.views++;
      }
      else{
        console.warn("Video not found for ID:", action.payload);
      }
      },
    incrementLocalLike(state, action) {
      const v = state.data.find((x) => x.id === action.payload);
      if (v) v.stats.likes++;
    },
    decrementLocalLike(state, action) {
      const v = state.data.find((x) => x.id === action.payload);
      if (v) v.stats.likes--;
    },
    // inside videoSlice, add to reducers:
    setVideo(state, action) {
      const video = action.payload;
      const index = state.data.findIndex((v) => v.id === video.id);
      if (index >= 0) {
        state.data[index] = video;
      } else {
        state.data.push(video);
      }
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchVideos.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchVideos.fulfilled, (state, action) => {
        state.data = action.payload;
        state.loading = false;
        console.log("Fetched Videos:", action.payload);
      });
  },
});

export const { incrementLocalView, incrementLocalLike, decrementLocalLike, setVideo } =
  videoSlice.actions;

export default videoSlice.reducer;
