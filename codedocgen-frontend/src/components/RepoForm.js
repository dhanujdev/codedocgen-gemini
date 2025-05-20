import React, { useState } from 'react';
import { TextField, Button, Box, CircularProgress } from '@mui/material';

function RepoForm({ onAnalyze, isLoading }) {
  const [repoUrl, setRepoUrl] = useState('');

  const handleSubmit = (event) => {
    event.preventDefault();
    if (repoUrl.trim()) {
      onAnalyze(repoUrl);
    }
  };

  return (
    <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
      <TextField
        label="Git Repository URL"
        variant="outlined"
        fullWidth
        value={repoUrl}
        onChange={(e) => setRepoUrl(e.target.value)}
        disabled={isLoading}
        placeholder="e.g., https://github.com/user/repo.git"
      />
      <Button 
        type="submit" 
        variant="contained" 
        color="primary" 
        disabled={isLoading || !repoUrl.trim()}
        sx={{ minWidth: 120, height: 56}} // Match TextField height
      >
        {isLoading ? <CircularProgress size={24} color="inherit" /> : 'Analyze'}
      </Button>
    </Box>
  );
}

export default RepoForm; 