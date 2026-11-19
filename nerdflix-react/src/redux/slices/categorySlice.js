import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import api from "../../api/axiosInstance";

export const fetchCategories = createAsyncThunk(
    "categories/fetch",
    async () => {
        const res = await api.get("/category");
        return res.data;
    }
);

// console.log("Checking Thunk:", fetchCategories); 

const categorySlice = createSlice({
    name: "categories",
    initialState: {
        data: [],
        loading: false
    },
    extraReducers: (builder) => {
        builder
            .addCase(fetchCategories.pending, (state) => {
                state.loading = true;
            })
            .addCase(fetchCategories.fulfilled, (state, action) => {
                state.data = action.payload;
                state.loading = false;
            });
    }
});

export default categorySlice.reducer;
