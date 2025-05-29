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
    <Box 
      component="form" 
      onSubmit={handleSubmit} 
      sx={{
        width: '100%', // Ensure the form itself takes full available width
        display: 'flex',
        flexDirection: { xs: 'column', sm: 'row' }, // Stack on extra-small screens, row on small and up
        gap: 2, 
        alignItems: { sm: 'center' } // Align items center only when in a row
      }}
    >
      <TextField
        label="Git Repository URL"
        variant="outlined"
        fullWidth // Will now take full width of the 100% width Box
        value={repoUrl}
        onChange={(e) => setRepoUrl(e.target.value)}
        disabled={isLoading}
        placeholder="e.g., https://github.com/user/repo.git"
        sx={{ mb: { xs: 2, sm: 0 } }} // Add margin bottom on extra-small screens when stacked
      />
      <Button 
        type="submit" 
        variant="contained" 
        color="primary" 
        disabled={isLoading || !repoUrl.trim()}
        sx={{
          minWidth: { xs: '100%', sm: 120 }, // Full width on extra-small screens
          height: 56
        }}
      >
        {isLoading ? <CircularProgress size={24} color="inherit" /> : 'Analyze'}
      </Button>
    </Box>
  );
}

export default RepoForm; 